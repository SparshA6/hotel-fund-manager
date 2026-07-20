const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { google } = require('googleapis');
const multer = require('multer');
const xlsx = require('xlsx');
const ExcelJS = require('exceljs');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json({ limit: '15mb' }));

// Static uploads configuration
const uploadsDir = path.join(__dirname, 'public', 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}
app.use('/uploads', express.static(uploadsDir));

// Request logging middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  if (req.method === 'POST') {
    console.log('Body:', JSON.stringify(req.body, null, 2));
  }
  next();
});

// Determine database mode
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/hotel_fund';
let isUsingMongoDB = false;
const JSON_FILE_PATH = path.join(__dirname, 'bookings.json');

// Helper functions for local JSON file fallback
function readLocalBookings() {
  try {
    if (!fs.existsSync(JSON_FILE_PATH)) {
      return [];
    }
    const data = fs.readFileSync(JSON_FILE_PATH, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    console.error('Error reading local bookings file:', error);
    return [];
  }
}

function writeLocalBookings(bookings) {
  try {
    fs.writeFileSync(JSON_FILE_PATH, JSON.stringify(bookings, null, 2), 'utf8');
  } catch (error) {
    console.error('Error writing local bookings file:', error);
  }
}

const BACKUPS_FILE_PATH = path.join(__dirname, 'backups.json');

function readLocalBackups() {
  try {
    if (!fs.existsSync(BACKUPS_FILE_PATH)) {
      return [];
    }
    const data = fs.readFileSync(BACKUPS_FILE_PATH, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    console.error('Error reading local backups file:', error);
    return [];
  }
}

function writeLocalBackups(backups) {
  try {
    fs.writeFileSync(BACKUPS_FILE_PATH, JSON.stringify(backups, null, 2), 'utf8');
  } catch (error) {
    console.error('Error writing local backups file:', error);
  }
}

const PORTAL_SETTINGS_FILE_PATH = path.join(__dirname, 'portal_settings.json');

function readLocalPortalSettings() {
  try {
    if (!fs.existsSync(PORTAL_SETTINGS_FILE_PATH)) {
      return [];
    }
    const data = fs.readFileSync(PORTAL_SETTINGS_FILE_PATH, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    console.error('Error reading local portal settings file:', error);
    return [];
  }
}

function writeLocalPortalSettings(settings) {
  try {
    fs.writeFileSync(PORTAL_SETTINGS_FILE_PATH, JSON.stringify(settings, null, 2), 'utf8');
  } catch (error) {
    console.error('Error writing local portal settings file:', error);
  }
}

const STATEMENTS_FILE_PATH = path.join(__dirname, 'statements.json');
const UPLOADED_FILES_FILE_PATH = path.join(__dirname, 'uploaded_files.json');

function readLocalStatements() {
  try {
    if (!fs.existsSync(STATEMENTS_FILE_PATH)) {
      return [];
    }
    const data = fs.readFileSync(STATEMENTS_FILE_PATH, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    console.error('Error reading local statements file:', error);
    return [];
  }
}

function writeLocalStatements(statements) {
  try {
    fs.writeFileSync(STATEMENTS_FILE_PATH, JSON.stringify(statements, null, 2), 'utf8');
  } catch (error) {
    console.error('Error writing local statements file:', error);
  }
}

function readLocalUploadedFiles() {
  try {
    if (!fs.existsSync(UPLOADED_FILES_FILE_PATH)) {
      return [];
    }
    const data = fs.readFileSync(UPLOADED_FILES_FILE_PATH, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    console.error('Error reading local uploaded files file:', error);
    return [];
  }
}

function writeLocalUploadedFiles(files) {
  try {
    fs.writeFileSync(UPLOADED_FILES_FILE_PATH, JSON.stringify(files, null, 2), 'utf8');
  } catch (error) {
    console.error('Error writing local uploaded files file:', error);
  }
}

async function cleanupReconciliation(bookingId, bookingData) {
  try {
    let statements = [];
    if (isUsingMongoDB) {
      statements = await StatementRecord.find({ matchedBookingId: bookingId });
    } else {
      statements = readLocalStatements().filter(s => s.matchedBookingId === bookingId);
    }

    if (statements.length === 0) return;

    const updatedStatements = isUsingMongoDB ? [] : [...readLocalStatements()];

    for (let statement of statements) {
      let stillValid = false;
      if (bookingData && bookingData.payments && Array.isArray(bookingData.payments)) {
        const payment = bookingData.payments.find(p => p.id === statement.matchedPaymentId);
        if (payment && Math.abs((parseFloat(payment.amount) || 0) - statement.amount) < 0.01) {
          stillValid = true;
        }
      }

      if (!stillValid) {
        statement.isMatched = false;
        statement.matchedBookingId = '';
        statement.matchedPaymentId = '';
        statement.description = statement.description.replace(/\s*\[Matched:.*\]$/, '');

        if (isUsingMongoDB) {
          await StatementRecord.updateOne(
            { id: statement.id },
            {
              isMatched: false,
              matchedBookingId: '',
              matchedPaymentId: '',
              description: statement.description
            }
          );
        } else {
          const idx = updatedStatements.findIndex(s => s.id === statement.id);
          if (idx !== -1) {
            updatedStatements[idx] = statement;
          }
        }
      }
    }

    if (!isUsingMongoDB) {
      writeLocalStatements(updatedStatements);
    }
  } catch (error) {
    console.error('Error cleaning up reconciliation:', error);
  }
}

// Connect to MongoDB Atlas
const maskedURI = MONGODB_URI.replace(/:([^:@]+)@/, ':***@');
console.log('Attempting to connect to MongoDB URI:', maskedURI);

mongoose.connect(MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB successfully!');
    isUsingMongoDB = true;
    seedPortalSettings();
  })
  .catch((err) => {
    console.warn('MongoDB connection failed. Falling back to local JSON file storage.');
    console.warn('Error details:', err.message);
    isUsingMongoDB = false;
    seedPortalSettings();
  });

// MongoDB Mongoose Schemas
const BookingItemSchema = new mongoose.Schema({
  id: { type: String, required: true },
  category: { type: String, required: true },
  roomNumber: { type: String, default: "" },
  amount: { type: Number, required: true },
  nights: { type: Number, default: 1 },
  rates: [{ type: Number }],
  startDate: { type: String, default: "" }
});

const PaymentDetailSchema = new mongoose.Schema({
  id: { type: String, required: true },
  amount: { type: Number, required: true },
  method: { type: String, required: true },
  timestamp: { type: Number, required: true }
});

const GuestIdImageSchema = new mongoose.Schema({
  id: { type: String, required: true },
  url: { type: String, required: true },
  fileId: { type: String, default: "" },
  label: { type: String, default: "" }
});

const GuestIdCardSchema = new mongoose.Schema({
  id: { type: String, required: true },
  idType: { type: String, required: true },
  guestName: { type: String, default: "" },
  images: [GuestIdImageSchema]
});

const BookingSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  checkInDate: { type: String, required: true },
  platform: { type: String, required: true },
  guestName: { type: String, required: true },
  items: [BookingItemSchema],
  dormBedsSelected: { type: Number, default: 0 },
  dormRoomABeds: { type: String, default: "" },
  dormRoomBBeds: { type: String, default: "" },
  dormTotalAmount: { type: Number, default: 0.0 },
  isBillOn: { type: Boolean, required: true },
  billAmount: { type: Number, required: true },
  expenses: { type: Number, default: 0.0 },
  paymentStatus: { type: String, required: true },
  paymentMethod: { type: String, required: true },
  payments: [PaymentDetailSchema],
  notes: { type: String, default: "" },
  discount: { type: Number, default: 0.0 },
  extraPrice: { type: Number, default: 0.0 },
  idDocumentType: { type: String, default: "" },
  idDocumentUrl: { type: String, default: "" },
  idDocumentFileId: { type: String, default: "" },
  guestIds: { type: [GuestIdCardSchema], default: [] },
  timestamp: { type: Number, required: true }
});

const Booking = mongoose.model('Booking', BookingSchema);

const BackupSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  timestamp: { type: Number, required: true },
  displayDate: { type: String, required: true },
  bookingCount: { type: Number, required: true },
  bookings: { type: Array, default: [] }
});

const Backup = mongoose.model('Backup', BackupSchema);

const PortalSettingsSchema = new mongoose.Schema({
  platform: { type: String, required: true, unique: true },
  commissionRate: { type: Number, default: 0.0 },
  propertyGstRate: { type: Number, default: 0.0 },
  gstOnCommissionRate: { type: Number, default: 0.0 },
  tdsRate: { type: Number, default: 0.0 },
  tcsRate: { type: Number, default: 0.0 },
  paymentProcessingFeeRate: { type: Number, default: 0.0 },
  serviceCharge: { type: Number, default: 0.0 }
});

const PortalSettings = mongoose.model('PortalSettings', PortalSettingsSchema);

const StatementRecordSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  date: { type: String, required: true },
  description: { type: String, required: true },
  amount: { type: Number, required: true },
  isMatched: { type: Boolean, default: false },
  matchedBookingId: { type: String, default: "" },
  matchedPaymentId: { type: String, default: "" },
  fileIds: { type: [String], default: [] }
});

const StatementRecord = mongoose.model('StatementRecord', StatementRecordSchema);

const UploadedFileSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  filename: { type: String, required: true },
  originalName: { type: String, required: true },
  uploadDate: { type: String, required: true },
  filePath: { type: String, required: true },
  startDate: { type: String, default: "" },
  endDate: { type: String, default: "" }
});

const UploadedFile = mongoose.model('UploadedFile', UploadedFileSchema);

const DEFAULT_PORTAL_SETTINGS = [
  { platform: "MMT", commissionRate: 20.0, propertyGstRate: 12.0, gstOnCommissionRate: 18.0, tdsRate: 1.0, tcsRate: 1.0, paymentProcessingFeeRate: 0.0, serviceCharge: 0.0 },
  { platform: "Goibibo", commissionRate: 15.0, propertyGstRate: 12.0, gstOnCommissionRate: 18.0, tdsRate: 1.0, tcsRate: 1.0, paymentProcessingFeeRate: 0.0, serviceCharge: 0.0 },
  { platform: "Yatra", commissionRate: 15.0, propertyGstRate: 0.0, gstOnCommissionRate: 0.0, tdsRate: 1.0, tcsRate: 1.0, paymentProcessingFeeRate: 0.0, serviceCharge: 0.0 },
  { platform: "Booking.com", commissionRate: 15.0, propertyGstRate: 12.0, gstOnCommissionRate: 0.0, tdsRate: 0.0, tcsRate: 0.0, paymentProcessingFeeRate: 2.0, serviceCharge: 0.0 },
  { platform: "Agoda", commissionRate: 15.0, propertyGstRate: 12.0, gstOnCommissionRate: 18.0, tdsRate: 1.0, tcsRate: 1.0, paymentProcessingFeeRate: 0.0, serviceCharge: 0.0 },
  { platform: "Cleartrip", commissionRate: 12.0, propertyGstRate: 12.0, gstOnCommissionRate: 18.0, tdsRate: 1.0, tcsRate: 1.0, paymentProcessingFeeRate: 0.0, serviceCharge: 0.0 }
];

async function seedPortalSettings() {
  try {
    if (isUsingMongoDB) {
      const count = await PortalSettings.countDocuments();
      if (count === 0) {
        await PortalSettings.insertMany(DEFAULT_PORTAL_SETTINGS);
        console.log("Portal settings seeded in MongoDB successfully.");
      }
    } else {
      const settings = readLocalPortalSettings();
      if (settings.length === 0) {
        writeLocalPortalSettings(DEFAULT_PORTAL_SETTINGS);
        console.log("Portal settings seeded in local JSON file successfully.");
      }
    }
  } catch (error) {
    console.error("Error seeding portal settings:", error);
  }
}

// REST API Endpoints

// 0. Portal settings endpoints
app.get('/api/portal-settings', async (req, res) => {
  try {
    if (isUsingMongoDB) {
      const settings = await PortalSettings.find();
      res.json(settings);
    } else {
      const settings = readLocalPortalSettings();
      res.json(settings);
    }
  } catch (error) {
    console.error('Error fetching portal settings:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

app.post('/api/portal-settings', async (req, res) => {
  try {
    const data = req.body;
    if (!data.platform) {
      return res.status(400).json({ error: 'Missing required field: platform' });
    }
    if (isUsingMongoDB) {
      const updated = await PortalSettings.findOneAndUpdate(
        { platform: data.platform },
        data,
        { new: true, upsert: true, runValidators: true }
      );
      res.json(updated);
    } else {
      const settings = readLocalPortalSettings();
      const index = settings.findIndex(s => s.platform === data.platform);
      if (index !== -1) {
        settings[index] = data;
      } else {
        settings.push(data);
      }
      writeLocalPortalSettings(settings);
      res.json(data);
    }
  } catch (error) {
    console.error('Error saving portal settings:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 1. Get all bookings
app.get('/api/bookings', async (req, res) => {
  try {
    if (isUsingMongoDB) {
      const bookings = await Booking.find().sort({ timestamp: -1 });
      res.json(bookings);
    } else {
      const bookings = readLocalBookings();
      // Sort descending by timestamp
      bookings.sort((a, b) => b.timestamp - a.timestamp);
      res.json(bookings);
    }
  } catch (error) {
    console.error('Error fetching bookings:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 2. Save or update a booking
app.post('/api/bookings', async (req, res) => {
  try {
    const bookingData = req.body;
    if (!bookingData.id || !bookingData.checkInDate) {
      return res.status(400).json({ error: 'Missing required fields (id, checkInDate)' });
    }

    if (isUsingMongoDB) {
      // Upsert: update if exists, insert if not
      const updatedBooking = await Booking.findOneAndUpdate(
        { id: bookingData.id },
        bookingData,
        { new: true, upsert: true, runValidators: true }
      );
      await cleanupReconciliation(bookingData.id, bookingData);
      res.json(updatedBooking);
    } else {
      const bookings = readLocalBookings();
      const index = bookings.findIndex(b => b.id === bookingData.id);
      if (index !== -1) {
        bookings[index] = bookingData;
      } else {
        bookings.push(bookingData);
      }
      writeLocalBookings(bookings);
      await cleanupReconciliation(bookingData.id, bookingData);
      res.json(bookingData);
    }
  } catch (error) {
    console.error('Error saving booking:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 3. Delete a booking
app.delete('/api/bookings/:id', async (req, res) => {
  try {
    const bookingId = req.params.id;
    const deleteIds = req.query.deleteIds === 'true';
    
    let bookingToDelete = null;

    if (isUsingMongoDB) {
      if (deleteIds) {
        bookingToDelete = await Booking.findOne({ id: bookingId });
      }
      const result = await Booking.findOneAndDelete({ id: bookingId });
      if (!result) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      await cleanupReconciliation(bookingId, null);
      res.json({ message: 'Booking deleted successfully', id: bookingId });
    } else {
      const bookings = readLocalBookings();
      const index = bookings.findIndex(b => b.id === bookingId);
      if (index === -1) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      bookingToDelete = bookings[index];
      const filtered = bookings.filter(b => b.id !== bookingId);
      writeLocalBookings(filtered);
      await cleanupReconciliation(bookingId, null);
      res.json({ message: 'Booking deleted successfully', id: bookingId });
    }

    // Clean up associated Guest ID files if requested
    if (deleteIds && bookingToDelete && bookingToDelete.guestIds) {
      for (const card of bookingToDelete.guestIds) {
        for (const img of card.images) {
          if (img.fileId) {
            deleteFileFromGoogleDrive(img.fileId).catch(err => console.error("Error cleaning up Drive file:", err));
          }
          if (img.url && img.url.includes('/uploads/')) {
            const urlParts = img.url.split('/uploads/');
            if (urlParts.length > 1) {
              const localPath = path.join(uploadsDir, urlParts[1]);
              if (fs.existsSync(localPath)) {
                try {
                  fs.unlinkSync(localPath);
                } catch (e) {
                  console.error("Error cleaning up local file:", e);
                }
              }
            }
          }
        }
      }
    }
  } catch (error) {
    console.error('Error deleting booking:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Backups Endpoints

// 1. Get all backups (metadata only)
app.get('/api/backups', async (req, res) => {
  try {
    if (isUsingMongoDB) {
      const backups = await Backup.find({}, { bookings: 0 }).sort({ timestamp: -1 });
      res.json(backups);
    } else {
      const backups = readLocalBackups();
      const metadata = backups.map(b => {
        const { bookings, ...meta } = b;
        return meta;
      });
      metadata.sort((a, b) => b.timestamp - a.timestamp);
      res.json(metadata);
    }
  } catch (error) {
    console.error('Error fetching backups:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 2. Create a new backup from current DB state
app.post('/api/backups', async (req, res) => {
  try {
    const timestamp = Date.now();
    const displayDate = new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' });
    const id = 'backup_' + timestamp;

    let bookings = [];
    if (isUsingMongoDB) {
      bookings = await Booking.find();
    } else {
      bookings = readLocalBookings();
    }

    const backupData = {
      id,
      timestamp,
      displayDate,
      bookingCount: bookings.length,
      bookings: bookings
    };

    if (isUsingMongoDB) {
      const newBackup = new Backup(backupData);
      await newBackup.save();
      const { bookings: _, ...meta } = backupData;
      res.json(meta);
    } else {
      const backups = readLocalBackups();
      backups.push(backupData);
      writeLocalBackups(backups);
      const { bookings: _, ...meta } = backupData;
      res.json(meta);
    }
  } catch (error) {
    console.error('Error creating backup:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 3. Restore a backup
app.post('/api/backups/:id/restore', async (req, res) => {
  try {
    const backupId = req.params.id;
    let backupDoc = null;

    if (isUsingMongoDB) {
      backupDoc = await Backup.findOne({ id: backupId });
    } else {
      const backups = readLocalBackups();
      backupDoc = backups.find(b => b.id === backupId);
    }

    if (!backupDoc) {
      return res.status(404).json({ error: 'Backup not found' });
    }

    if (isUsingMongoDB) {
      await Booking.deleteMany({});
      if (backupDoc.bookings && backupDoc.bookings.length > 0) {
        await Booking.insertMany(backupDoc.bookings);
      }
    } else {
      writeLocalBookings(backupDoc.bookings || []);
    }

    res.json({ message: 'Backup restored successfully', bookingCount: backupDoc.bookingCount });
  } catch (error) {
    console.error('Error restoring backup:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// 4. Delete a backup
app.delete('/api/backups/:id', async (req, res) => {
  try {
    const backupId = req.params.id;

    if (isUsingMongoDB) {
      const result = await Backup.findOneAndDelete({ id: backupId });
      if (!result) {
        return res.status(404).json({ error: 'Backup not found' });
      }
      res.json({ message: 'Backup deleted successfully', id: backupId });
    } else {
      const backups = readLocalBackups();
      const filtered = backups.filter(b => b.id !== backupId);
      if (backups.length === filtered.length) {
        return res.status(404).json({ error: 'Backup not found' });
      }
      writeLocalBackups(filtered);
      res.json({ message: 'Backup deleted successfully', id: backupId });
    }
  } catch (error) {
    console.error('Error deleting backup:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

async function getOrCreateFolder(drive, folderName, parentId) {
  const qParts = [
    `name = '${folderName}'`,
    `mimeType = 'application/vnd.google-apps.folder'`,
    `trashed = false`
  ];
  if (parentId) {
    qParts.push(`'${parentId}' in parents`);
  }
  const response = await drive.files.list({
    q: qParts.join(' and '),
    fields: 'files(id)'
  });
  if (response.data.files && response.data.files.length > 0) {
    return response.data.files[0].id;
  }
  
  const fileMetadata = {
    name: folderName,
    mimeType: 'application/vnd.google-apps.folder',
    parents: parentId ? [parentId] : []
  };
  const folder = await drive.files.create({
    resource: fileMetadata,
    fields: 'id'
  });
  return folder.data.id;
}

async function uploadToGoogleDrive(buffer, fileName, mimeType, folders = []) {
  let auth = null;
  
  if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET && process.env.GOOGLE_REFRESH_TOKEN) {
    const oauth2Client = new google.auth.OAuth2(
      process.env.GOOGLE_CLIENT_ID,
      process.env.GOOGLE_CLIENT_SECRET,
      'http://localhost'
    );
    oauth2Client.setCredentials({
      refresh_token: process.env.GOOGLE_REFRESH_TOKEN
    });
    auth = oauth2Client;
    console.log("Using OAuth2 User Authentication for Google Drive.");
  } else if (process.env.GOOGLE_CREDENTIALS) {
    try {
      const credentials = JSON.parse(process.env.GOOGLE_CREDENTIALS);
      auth = new google.auth.JWT(
        credentials.client_email,
        null,
        credentials.private_key,
        ['https://www.googleapis.com/auth/drive.file', 'https://www.googleapis.com/auth/drive']
      );
      console.log("Using Service Account Authentication for Google Drive.");
    } catch (e) {
      console.error("Failed to parse GOOGLE_CREDENTIALS:", e);
    }
  }

  if (!auth) {
    console.warn("Google Drive credentials (OAuth2 or Service Account) are not set. Google Drive upload is skipped.");
    return null;
  }

  try {
    const drive = google.drive({ version: 'v3', auth });
    
    // Resolve target folder hierarchy
    let currentParentId = process.env.GOOGLE_DRIVE_FOLDER_ID || null;
    for (const folderName of folders) {
      currentParentId = await getOrCreateFolder(drive, folderName, currentParentId);
    }
    
    const { Readable } = require('stream');
    const stream = new Readable();
    stream.push(buffer);
    stream.push(null);
    
    const fileMetadata = {
      name: fileName,
      parents: currentParentId ? [currentParentId] : []
    };
    const media = {
      mimeType: mimeType,
      body: stream
    };
    
    console.log("Uploading file to Google Drive...");
    const file = await drive.files.create({
      resource: fileMetadata,
      media: media,
      fields: 'id, webViewLink, webContentLink'
    });
    
    console.log("File uploaded successfully to Drive. File ID:", file.data.id);
    
    try {
      await drive.permissions.create({
        fileId: file.data.id,
        requestBody: {
          role: 'reader',
          type: 'anyone'
        }
      });
      console.log("Drive file permissions updated to anyone/reader.");
    } catch (permError) {
      console.warn("Could not share file publicly:", permError.message);
    }
    
    return {
      url: `https://drive.google.com/uc?export=download&id=${file.data.id}`,
      fileId: file.data.id
    };
  } catch (error) {
    console.error("Google Drive upload failed:", error);
    return null;
  }
}

app.post('/api/bookings/:id/upload-id', async (req, res) => {
  try {
    const bookingId = req.params.id;
    const { idDocumentType, imageContentBase64, imageName } = req.body;
    
    if (!idDocumentType || !imageContentBase64) {
      return res.status(400).json({ error: 'Missing required fields (idDocumentType, imageContentBase64)' });
    }
    
    // Parse MIME type and extract base64 raw string
    let base64Data = imageContentBase64;
    let mimeType = 'image/jpeg';
    if (base64Data.startsWith('data:')) {
      const matches = base64Data.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
      if (matches && matches.length === 3) {
        mimeType = matches[1];
        base64Data = matches[2];
      }
    }
    
    const buffer = Buffer.from(base64Data, 'base64');
    const extension = mimeType.split('/')[1] || 'jpg';
    const cleanFileName = `id_${bookingId}_${Date.now()}.${extension}`;
    
    // 1. Try uploading to Google Drive
    const driveResult = await uploadToGoogleDrive(buffer, cleanFileName, mimeType);
    let documentUrl = driveResult ? driveResult.url : null;
    let documentFileId = driveResult ? driveResult.fileId : "";
    
    // 2. If Drive upload failed/skipped, save locally as fallback
    if (!documentUrl) {
      console.log("Falling back to local static file storage.");
      const localPath = path.join(uploadsDir, cleanFileName);
      fs.writeFileSync(localPath, buffer);
      
      const protocol = req.headers['x-forwarded-proto'] || req.protocol;
      const host = req.get('host');
      documentUrl = `${protocol}://${host}/uploads/${cleanFileName}`;
    }
    
    console.log(`Document URL generated: ${documentUrl}, File ID: ${documentFileId}`);
    
    // 3. Update the booking in Database/JSON file
    let updatedBooking = null;
    if (isUsingMongoDB) {
      updatedBooking = await Booking.findOneAndUpdate(
        { id: bookingId },
        { idDocumentType, idDocumentUrl: documentUrl, idDocumentFileId: documentFileId },
        { new: true }
      );
    } else {
      const bookings = readLocalBookings();
      const index = bookings.findIndex(b => b.id === bookingId);
      if (index !== -1) {
        bookings[index].idDocumentType = idDocumentType;
        bookings[index].idDocumentUrl = documentUrl;
        bookings[index].idDocumentFileId = documentFileId;
        writeLocalBookings(bookings);
        updatedBooking = bookings[index];
      }
    }
    
    if (!updatedBooking) {
      return res.status(404).json({ error: 'Booking not found' });
    }
    
    res.json(updatedBooking);
  } catch (error) {
    console.error('Error uploading ID document:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

async function deleteFileFromGoogleDrive(fileId) {
  let auth = null;
  if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET && process.env.GOOGLE_REFRESH_TOKEN) {
    const oauth2Client = new google.auth.OAuth2(
      process.env.GOOGLE_CLIENT_ID,
      process.env.GOOGLE_CLIENT_SECRET,
      'http://localhost'
    );
    oauth2Client.setCredentials({ refresh_token: process.env.GOOGLE_REFRESH_TOKEN });
    auth = oauth2Client;
  } else if (process.env.GOOGLE_CREDENTIALS) {
    try {
      const credentials = JSON.parse(process.env.GOOGLE_CREDENTIALS);
      auth = new google.auth.JWT(
        credentials.client_email,
        null,
        credentials.private_key,
        ['https://www.googleapis.com/auth/drive']
      );
    } catch (e) {
      console.error(e);
    }
  }
  if (!auth) return;
  try {
    const drive = google.drive({ version: 'v3', auth });
    await drive.files.delete({ fileId });
    console.log(`Successfully deleted file from Google Drive. File ID: ${fileId}`);
  } catch (err) {
    console.error(`Error deleting Google Drive file ${fileId}:`, err.message);
  }
}

// New upload endpoint for multiple guest IDs
app.post('/api/bookings/:id/guest-ids/upload', async (req, res) => {
  try {
    const bookingId = req.params.id;
    const { cardId, imageId, idType, guestName, imageContentBase64, label, index } = req.body;
    
    if (!cardId || !imageId || !idType || !imageContentBase64) {
      return res.status(400).json({ error: 'Missing required fields (cardId, imageId, idType, imageContentBase64)' });
    }
    
    // Parse MIME type and extract base64 raw string
    let base64Data = imageContentBase64;
    let mimeType = 'image/jpeg';
    if (base64Data.startsWith('data:')) {
      const matches = base64Data.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
      if (matches && matches.length === 3) {
        mimeType = matches[1];
        base64Data = matches[2];
      }
    }
    
    const buffer = Buffer.from(base64Data, 'base64');
    const extension = mimeType.split('/')[1] || 'jpg';
    
    // Structured folder names (Year/Month/Date)
    const now = new Date();
    const yyyy = now.getFullYear().toString();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const dateStr = `${yyyy}-${mm}-${dd}`;
    const folders = [yyyy, mm, dd];
    
    // File name: date_bookingId_index.ext
    const formattedFileName = `${dateStr}_${bookingId}_${index}.${extension}`;
    
    // 1. Try uploading to Google Drive inside the structured folders
    const driveResult = await uploadToGoogleDrive(buffer, formattedFileName, mimeType, folders);
    let documentUrl = driveResult ? driveResult.url : null;
    let documentFileId = driveResult ? driveResult.fileId : "";
    
    // 2. Fallback to local storage (structured uploads/YYYY/MM/DD/)
    if (!documentUrl) {
      console.log("Falling back to local structured static storage.");
      const relativePath = path.join(yyyy, mm, dd);
      const targetDir = path.join(uploadsDir, relativePath);
      if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
      }
      const localPath = path.join(targetDir, formattedFileName);
      fs.writeFileSync(localPath, buffer);
      
      const protocol = req.headers['x-forwarded-proto'] || req.protocol;
      const host = req.get('host');
      documentUrl = `${protocol}://${host}/uploads/${yyyy}/${mm}/${dd}/${formattedFileName}`;
    }
    
    console.log(`Document URL generated: ${documentUrl}, File ID: ${documentFileId}`);
    
    // 3. Update target booking
    let updatedBooking = null;
    if (isUsingMongoDB) {
      const booking = await Booking.findOne({ id: bookingId });
      if (!booking) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      
      let card = booking.guestIds.find(c => c.id === cardId);
      if (!card) {
        card = { id: cardId, idType, guestName: guestName || "", images: [] };
        booking.guestIds.push(card);
        card = booking.guestIds[booking.guestIds.length - 1];
      }
      
      let img = card.images.find(i => i.id === imageId);
      if (img) {
        img.url = documentUrl;
        img.fileId = documentFileId;
        img.label = label || "";
      } else {
        card.images.push({ id: imageId, url: documentUrl, fileId: documentFileId, label: label || "" });
      }
      
      updatedBooking = await booking.save();
    } else {
      const bookings = readLocalBookings();
      const bIndex = bookings.findIndex(b => b.id === bookingId);
      if (bIndex !== -1) {
        let booking = bookings[bIndex];
        if (!booking.guestIds) {
          booking.guestIds = [];
        }
        let card = booking.guestIds.find(c => c.id === cardId);
        if (!card) {
          card = { id: cardId, idType, guestName: guestName || "", images: [] };
          booking.guestIds.push(card);
        }
        let img = card.images.find(i => i.id === imageId);
        if (img) {
          img.url = documentUrl;
          img.fileId = documentFileId;
          img.label = label || "";
        } else {
          card.images.push({ id: imageId, url: documentUrl, fileId: documentFileId, label: label || "" });
        }
        writeLocalBookings(bookings);
        updatedBooking = booking;
      }
    }
    
    if (!updatedBooking) {
      return res.status(404).json({ error: 'Booking not found' });
    }
    
    res.json(updatedBooking);
  } catch (error) {
    console.error('Error uploading guest ID image:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Delete specific ID image route
app.delete('/api/bookings/:id/guest-ids/:cardId/images/:imageId', async (req, res) => {
  try {
    const { id: bookingId, cardId, imageId } = req.params;
    
    let fileIdToDelete = null;
    let localPathToDelete = null;
    let updatedBooking = null;
    
    if (isUsingMongoDB) {
      const booking = await Booking.findOne({ id: bookingId });
      if (!booking) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      
      const card = booking.guestIds.find(c => c.id === cardId);
      if (card) {
        const imgIndex = card.images.findIndex(i => i.id === imageId);
        if (imgIndex !== -1) {
          const img = card.images[imgIndex];
          fileIdToDelete = img.fileId;
          if (img.url && img.url.includes('/uploads/')) {
            const urlParts = img.url.split('/uploads/');
            if (urlParts.length > 1) {
              localPathToDelete = path.join(uploadsDir, urlParts[1]);
            }
          }
          card.images.splice(imgIndex, 1);
        }
      }
      updatedBooking = await booking.save();
    } else {
      const bookings = readLocalBookings();
      const index = bookings.findIndex(b => b.id === bookingId);
      if (index !== -1) {
        const booking = bookings[index];
        if (booking.guestIds) {
          const card = booking.guestIds.find(c => c.id === cardId);
          if (card) {
            const imgIndex = card.images.findIndex(i => i.id === imageId);
            if (imgIndex !== -1) {
              const img = card.images[imgIndex];
              fileIdToDelete = img.fileId;
              if (img.url && img.url.includes('/uploads/')) {
                const urlParts = img.url.split('/uploads/');
                if (urlParts.length > 1) {
                  localPathToDelete = path.join(uploadsDir, urlParts[1]);
                }
              }
              card.images.splice(imgIndex, 1);
            }
          }
        }
        writeLocalBookings(bookings);
        updatedBooking = booking;
      }
    }
    
    if (!updatedBooking) {
      return res.status(404).json({ error: 'Booking not found' });
    }
    
    // Background delete files
    if (fileIdToDelete) {
      deleteFileFromGoogleDrive(fileIdToDelete).catch(err => console.error("Error deleting Drive file:", err));
    }
    if (localPathToDelete && fs.existsSync(localPathToDelete)) {
      try {
        fs.unlinkSync(localPathToDelete);
        console.log(`Deleted local file: ${localPathToDelete}`);
      } catch (err) {
        console.error("Error deleting local file:", err);
      }
    }
    
    res.json(updatedBooking);
  } catch (error) {
    console.error('Error deleting ID image:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Bank Statement Reconciliation Endpoints

// Helper function to format Excel date strings to YYYY-MM-DD
function formatDateString(rawDate) {
  if (!rawDate) return null;
  
  if (typeof rawDate === 'number') {
    const excelEpoch = new Date(1899, 11, 30);
    const msPerDay = 24 * 60 * 60 * 1000;
    const dateObj = new Date(excelEpoch.getTime() + rawDate * msPerDay);
    if (!isNaN(dateObj.getTime())) {
      return dateObj.toISOString().split('T')[0];
    }
  }

  const str = String(rawDate).trim();
  
  const dmy = str.match(/^(\d{1,2})[\/\-\.](\d{1,2})[\/\-\.](\d{4})$/);
  if (dmy) {
    const day = dmy[1].padStart(2, '0');
    const month = dmy[2].padStart(2, '0');
    const year = dmy[3];
    return `${year}-${month}-${day}`;
  }

  const ymd = str.match(/^(\d{4})[\/\-\.](\d{1,2})[\/\-\.](\d{1,2})$/);
  if (ymd) {
    const year = ymd[1];
    const month = ymd[2].padStart(2, '0');
    const day = ymd[3].padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  const parsedMs = Date.parse(str);
  if (!isNaN(parsedMs)) {
    return new Date(parsedMs).toISOString().split('T')[0];
  }

  return null;
}

// Helper to highlight matched rows in green inside the Excel file
async function updateExcelFileWithMatchedRows(filePath) {
  try {
    if (!fs.existsSync(filePath)) return;

    let matchedRecords = [];
    if (isUsingMongoDB) {
      matchedRecords = await StatementRecord.find({ isMatched: true });
    } else {
      matchedRecords = readLocalStatements().filter(s => s.isMatched);
    }

    const matchedSet = new Set();
    const getNormalizedDesc = (desc) => {
      return String(desc || '').replace(/\s*\[Matched:.*\]$/, '').trim().toLowerCase();
    };

    for (const r of matchedRecords) {
      const key = `${r.date}|${r.amount.toFixed(2)}|${getNormalizedDesc(r.description)}`;
      matchedSet.add(key);
    }

    const workbook = new ExcelJS.Workbook();
    await workbook.xlsx.readFile(filePath);

    workbook.eachSheet((worksheet) => {
      let headerRowIndex = -1;
      let colIndices = { date: -1, desc: -1, credit: -1, debit: -1, type: -1 };

      worksheet.eachRow({ includeEmpty: false }, (row, rowNumber) => {
        if (rowNumber <= 30 && headerRowIndex === -1) {
          let hasDate = false, hasDesc = false, hasAmount = false;
          row.eachCell({ includeEmpty: false }, (cell, colNumber) => {
            const val = String(cell.value || '').toLowerCase().trim();
            if (val.includes('date') || val === 'txn date' || val === 'value date') {
              hasDate = true; colIndices.date = colNumber;
            } else if (val.includes('description') || val.includes('particulars') || val.includes('narration') || val.includes('remarks') || val.includes('details')) {
              hasDesc = true; colIndices.desc = colNumber;
            } else if (val.includes('credit') || val === 'cr' || val.includes('deposit') || val === 'received' || val.includes('credited')) {
              hasAmount = true; colIndices.credit = colNumber;
            } else if (val.includes('debit') || val === 'dr' || val.includes('withdrawal') || val === 'paid' || val.includes('debited')) {
              colIndices.debit = colNumber;
            } else if (val === 'amount' || val === 'txn amount') {
              hasAmount = true;
              if (colIndices.credit === -1) colIndices.credit = colNumber;
            } else if (val.includes('type') || val === 'cr/dr' || val === 'cr_dr') {
              colIndices.type = colNumber;
            }
          });
          if (hasDate && hasDesc && hasAmount) {
            headerRowIndex = rowNumber;
            return;
          }
        }

        if (headerRowIndex !== -1 && rowNumber > headerRowIndex) {
          const dateCell = colIndices.date !== -1 ? row.getCell(colIndices.date) : null;
          const descCell = colIndices.desc !== -1 ? row.getCell(colIndices.desc) : null;
          const creditCell = colIndices.credit !== -1 ? row.getCell(colIndices.credit) : null;
          const debitCell = colIndices.debit !== -1 ? row.getCell(colIndices.debit) : null;
          const typeCell = colIndices.type !== -1 ? row.getCell(colIndices.type) : null;

          if (!dateCell || !descCell || !creditCell) return;

          let rawDate = dateCell.value;
          if (rawDate && typeof rawDate === 'object' && rawDate.result !== undefined) {
            rawDate = rawDate.result;
          }
          let rawDesc = descCell.value;
          if (rawDesc && typeof rawDesc === 'object' && rawDesc.result !== undefined) {
            rawDesc = rawDesc.result;
          }
          let rawCredit = creditCell.value;
          if (rawCredit && typeof rawCredit === 'object' && rawCredit.result !== undefined) {
            rawCredit = rawCredit.result;
          }

          if (!rawDate || !rawDesc) return;

          let creditAmt = parseFloat(String(rawCredit || '0').replace(/,/g, '')) || 0;
          let debitAmt = debitCell ? (parseFloat(String(debitCell.value || '0').replace(/,/g, '')) || 0) : 0;

          if (colIndices.type !== -1 && typeCell && typeCell.value) {
            const typeStr = String(typeCell.value).toUpperCase().trim();
            if (typeStr.includes('DR') || typeStr === 'D') {
              debitAmt = creditAmt;
              creditAmt = 0;
            }
          }

          if (creditAmt > 0 && debitAmt === 0) {
            const formattedDate = formatDateString(rawDate);
            if (formattedDate) {
              const key = `${formattedDate}|${creditAmt.toFixed(2)}|${getNormalizedDesc(rawDesc)}`;
              if (matchedSet.has(key)) {
                row.eachCell({ includeEmpty: true }, (cell) => {
                  cell.fill = {
                    type: 'pattern',
                    pattern: 'solid',
                    fgColor: { argb: 'FFC6EFCE' }
                  };
                  cell.font = {
                    ...(cell.font || {}),
                    color: { argb: 'FF006100' },
                    bold: true
                  };
                });
              } else {
                row.eachCell({ includeEmpty: true }, (cell) => {
                  if (cell.fill && cell.fill.fgColor && cell.fill.fgColor.argb === 'FFC6EFCE') {
                    cell.fill = undefined;
                    if (cell.font) {
                      cell.font = { ...cell.font, color: undefined, bold: false };
                    }
                  }
                });
              }
            }
          }
        }
      });
    });

    await workbook.xlsx.writeFile(filePath);
  } catch (error) {
    console.error('Error updating Excel file with matched rows:', error);
  }
}

// Main parser logic
function parseExcelStatement(buffer) {
  const workbook = xlsx.read(buffer, { type: 'buffer' });
  const records = [];

  for (const sheetName of workbook.SheetNames) {
    const sheet = workbook.Sheets[sheetName];
    const rows = xlsx.utils.sheet_to_json(sheet, { header: 1 });

    let headerIndex = -1;
    let colIndices = {
      date: -1,
      desc: -1,
      credit: -1,
      debit: -1,
      type: -1
    };

    for (let i = 0; i < Math.min(rows.length, 30); i++) {
      const row = rows[i];
      if (!row || !Array.isArray(row)) continue;

      let hasDate = false;
      let hasDesc = false;
      let hasAmount = false;

      for (let j = 0; j < row.length; j++) {
        const val = String(row[j] || '').toLowerCase().trim();
        if (val.includes('date') || val === 'txn date' || val === 'value date') {
          hasDate = true;
          colIndices.date = j;
        } else if (val.includes('description') || val.includes('particulars') || val.includes('narration') || val.includes('remarks') || val.includes('details')) {
          hasDesc = true;
          colIndices.desc = j;
        } else if (val.includes('credit') || val === 'cr' || val.includes('deposit') || val === 'received' || val.includes('credited')) {
          hasAmount = true;
          colIndices.credit = j;
        } else if (val.includes('debit') || val === 'dr' || val.includes('withdrawal') || val === 'paid' || val.includes('debited')) {
          colIndices.debit = j;
        } else if (val === 'amount' || val === 'txn amount') {
          hasAmount = true;
          if (colIndices.credit === -1) {
            colIndices.credit = j;
          }
        } else if (val.includes('type') || val === 'cr/dr' || val === 'cr_dr') {
          colIndices.type = j;
        }
      }

      if (hasDate && hasDesc && hasAmount) {
        headerIndex = i;
        break;
      }
    }

    if (headerIndex !== -1 && colIndices.date !== -1 && colIndices.desc !== -1 && colIndices.credit !== -1) {
      for (let i = headerIndex + 1; i < rows.length; i++) {
        const row = rows[i];
        if (!row || row.length === 0) continue;

        let rawDate = row[colIndices.date];
        let rawDesc = row[colIndices.desc];
        let rawCredit = row[colIndices.credit];
        let rawDebit = colIndices.debit !== -1 ? row[colIndices.debit] : null;
        let rawType = colIndices.type !== -1 ? row[colIndices.type] : '';

        if (!rawDate || !rawDesc) continue;

        let creditAmt = parseFloat(String(rawCredit || '0').replace(/,/g, '')) || 0;
        let debitAmt = rawDebit !== null ? (parseFloat(String(rawDebit || '0').replace(/,/g, '')) || 0) : 0;
        
        if (colIndices.type !== -1 && rawType) {
          const typeStr = String(rawType).toUpperCase().trim();
          if (typeStr.includes('DR') || typeStr === 'D') {
            debitAmt = creditAmt;
            creditAmt = 0;
          }
        }

        // Only parse deposit transactions (credits > 0, debits = 0)
        if (creditAmt > 0 && debitAmt === 0) {
          let formattedDate = formatDateString(rawDate);
          if (formattedDate) {
            records.push({
              date: formattedDate,
              description: String(rawDesc).trim(),
              amount: creditAmt
            });
          }
        }
      }
    }
  }

  return records;
}

const upload = multer({ limits: { fileSize: 10 * 1024 * 1024 } });

// Upload Endpoint
app.post('/api/statements/upload', upload.single('statement'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No statement file uploaded' });
    }

    // Save physical file on disk
    const uploadsDir = path.join(__dirname, 'uploads');
    if (!fs.existsSync(uploadsDir)) {
      fs.mkdirSync(uploadsDir, { recursive: true });
    }
    const originalName = req.file.originalname;
    const filename = `${Date.now()}-${originalName}`;
    const filePath = path.join(uploadsDir, filename);
    fs.writeFileSync(filePath, req.file.buffer);

    // Parse records from the uploaded file buffer
    const parsedRecords = parseExcelStatement(req.file.buffer);

    // Calculate start date and end date from parsed records
    const validDates = parsedRecords.map(r => r.date).filter(Boolean).sort();
    const startDate = validDates.length > 0 ? validDates[0] : '';
    const endDate = validDates.length > 0 ? validDates[validDates.length - 1] : '';

    // Save UploadedFile record
    const fileRecord = {
      id: require('crypto').randomUUID(),
      filename: filename,
      originalName: originalName,
      uploadDate: new Date().toISOString(),
      filePath: filePath,
      startDate: startDate,
      endDate: endDate
    };

    if (isUsingMongoDB) {
      await UploadedFile.create(fileRecord);
    } else {
      const files = readLocalUploadedFiles();
      files.push(fileRecord);
      writeLocalUploadedFiles(files);
    }

    // Deduplication check & fileIds tracking
    const existing = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    const getNormalizedDesc = (desc) => {
      return String(desc || '').replace(/\s*\[Matched:.*\]$/, '').trim().toLowerCase();
    };

    const statementsToSave = [];
    const existingToUpdate = [];

    for (const r of parsedRecords) {
      const extMatch = existing.find(ext =>
        ext.date === r.date &&
        Math.abs(ext.amount - r.amount) < 0.01 &&
        getNormalizedDesc(ext.description) === getNormalizedDesc(r.description)
      );

      if (extMatch) {
        let ids = Array.isArray(extMatch.fileIds) ? [...extMatch.fileIds] : [];
        if (!ids.includes(fileRecord.id)) {
          ids.push(fileRecord.id);
          extMatch.fileIds = ids;
          if (!existingToUpdate.some(item => item.id === extMatch.id)) {
            existingToUpdate.push(extMatch);
          }
        }
      } else {
        const batchMatch = statementsToSave.find(batchItem =>
          batchItem.date === r.date &&
          Math.abs(batchItem.amount - r.amount) < 0.01 &&
          getNormalizedDesc(batchItem.description) === getNormalizedDesc(r.description)
        );

        if (batchMatch) {
          if (!batchMatch.fileIds.includes(fileRecord.id)) {
            batchMatch.fileIds.push(fileRecord.id);
          }
        } else {
          statementsToSave.push({
            id: require('crypto').randomUUID(),
            date: r.date,
            description: r.description,
            amount: r.amount,
            isMatched: false,
            matchedBookingId: '',
            matchedPaymentId: '',
            fileIds: [fileRecord.id]
          });
        }
      }
    }

    if (isUsingMongoDB) {
      if (statementsToSave.length > 0) {
        await StatementRecord.insertMany(statementsToSave);
      }
      for (const item of existingToUpdate) {
        await StatementRecord.updateOne({ id: item.id }, { fileIds: item.fileIds });
      }
    } else {
      const existingList = readLocalStatements();
      for (const item of existingToUpdate) {
        const idx = existingList.findIndex(s => s.id === item.id);
        if (idx !== -1) existingList[idx].fileIds = item.fileIds;
      }
      const updated = [...existingList, ...statementsToSave];
      writeLocalStatements(updated);
    }

    const all = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    res.json(all);
  } catch (error) {
    console.error('Error uploading statement:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get statements Endpoint
app.get('/api/statements', async (req, res) => {
  try {
    const all = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    res.json(all);
  } catch (error) {
    console.error('Error fetching statements:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get uploaded statement files list Endpoint
app.get('/api/statements/files', async (req, res) => {
  try {
    const files = isUsingMongoDB ? await UploadedFile.find() : readLocalUploadedFiles();
    res.json(files);
  } catch (error) {
    console.error('Error fetching uploaded files:', error);
    res.status(500).json({ error: error.message });
  }
});

// View / Download Excel statement file with green-highlighted matched rows
app.get('/api/statements/files/:id/view', async (req, res) => {
  try {
    const fileId = req.params.id;
    let file = null;
    if (isUsingMongoDB) {
      file = await UploadedFile.findOne({ id: fileId });
    } else {
      const files = readLocalUploadedFiles();
      file = files.find(f => f.id === fileId);
    }

    if (!file || !fs.existsSync(file.filePath)) {
      return res.status(404).json({ error: 'File not found' });
    }

    // Update Excel file with green rows for matched statement entries
    await updateExcelFileWithMatchedRows(file.filePath);

    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `inline; filename="${file.originalName}"`);
    res.sendFile(file.filePath);
  } catch (error) {
    console.error('Error viewing statement file:', error);
    res.status(500).json({ error: error.message });
  }
});

// Delete specific statement file Endpoint
app.delete('/api/statements/files/:id', async (req, res) => {
  try {
    const fileId = req.params.id;
    let fileToDelete = null;

    if (isUsingMongoDB) {
      fileToDelete = await UploadedFile.findOne({ id: fileId });
    } else {
      const files = readLocalUploadedFiles();
      fileToDelete = files.find(f => f.id === fileId);
    }

    if (!fileToDelete) {
      return res.status(404).json({ error: 'Statement file not found' });
    }

    // Delete physical file from disk
    if (fs.existsSync(fileToDelete.filePath)) {
      try {
        fs.unlinkSync(fileToDelete.filePath);
      } catch (e) {
        console.error('Error deleting physical file:', e);
      }
    }

    // Delete UploadedFile record
    if (isUsingMongoDB) {
      await UploadedFile.deleteOne({ id: fileId });
    } else {
      const files = readLocalUploadedFiles().filter(f => f.id !== fileId);
      writeLocalUploadedFiles(files);
    }

    // Update StatementRecords: remove fileId reference
    let allStatements = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    const recordsToDelete = [];
    const recordsToUpdate = [];

    for (let stmt of allStatements) {
      let ids = Array.isArray(stmt.fileIds) ? [...stmt.fileIds] : [];
      if (ids.includes(fileId)) {
        ids = ids.filter(id => id !== fileId);
        if (ids.length === 0) {
          recordsToDelete.push(stmt.id);
        } else {
          stmt.fileIds = ids;
          recordsToUpdate.push(stmt);
        }
      }
    }

    if (isUsingMongoDB) {
      if (recordsToDelete.length > 0) {
        await StatementRecord.deleteMany({ id: { $in: recordsToDelete } });
      }
      for (let stmt of recordsToUpdate) {
        await StatementRecord.updateOne({ id: stmt.id }, { fileIds: stmt.fileIds });
      }
    } else {
      let localList = readLocalStatements();
      localList = localList.filter(s => !recordsToDelete.includes(s.id));
      for (let stmt of recordsToUpdate) {
        const idx = localList.findIndex(s => s.id === stmt.id);
        if (idx !== -1) {
          localList[idx].fileIds = stmt.fileIds;
        }
      }
      writeLocalStatements(localList);
    }

    const remaining = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    res.json(remaining);
  } catch (error) {
    console.error('Error deleting statement file:', error);
    res.status(500).json({ error: error.message });
  }
});

// Clear statements Endpoint
app.delete('/api/statements', async (req, res) => {
  try {
    // Delete files from uploads directory
    const files = isUsingMongoDB ? await UploadedFile.find() : readLocalUploadedFiles();
    for (const f of files) {
      if (fs.existsSync(f.filePath)) {
        try {
          fs.unlinkSync(f.filePath);
        } catch (e) {
          console.error('Error deleting file:', f.filePath, e);
        }
      }
    }

    if (isUsingMongoDB) {
      await StatementRecord.deleteMany();
      await UploadedFile.deleteMany();
    } else {
      writeLocalStatements([]);
      writeLocalUploadedFiles([]);
    }
    res.sendStatus(204);
  } catch (error) {
    console.error('Error clearing statements:', error);
    res.status(500).json({ error: error.message });
  }
});

// Match statements Endpoint
app.post('/api/statements/match', async (req, res) => {
  try {
    let statements = [];
    if (isUsingMongoDB) {
      statements = await StatementRecord.find({ isMatched: false });
    } else {
      statements = readLocalStatements().filter(s => !s.isMatched);
    }

    if (statements.length === 0) {
      const all = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
      return res.json(all);
    }

    let bookings = [];
    if (isUsingMongoDB) {
      bookings = await Booking.find();
    } else {
      bookings = readLocalBookings();
    }

    // Collect all payment IDs already used by matched statements
    // so the same payment cannot be matched to multiple statements
    let allStatements = [];
    if (isUsingMongoDB) {
      allStatements = await StatementRecord.find({ isMatched: true });
    } else {
      allStatements = readLocalStatements().filter(s => s.isMatched);
    }
    const alreadyMatchedPaymentIds = new Set(allStatements.map(s => s.matchedPaymentId).filter(Boolean));

    const paymentsPool = [];
    for (const b of bookings) {
      if (b.payments && Array.isArray(b.payments)) {
        for (const p of b.payments) {
          // Skip payments already matched to a statement
          if (alreadyMatchedPaymentIds.has(p.id)) continue;

          const methodStr = String(p.method || '').toLowerCase();
          if (methodStr.includes('gpay') || methodStr.includes('g-pay') || methodStr.includes('g pay') || methodStr.includes('hotel acc')) {
            // Use IST (UTC+5:30) for date extraction to match statement dates from Excel
            const pDate = new Date(p.timestamp);
            const istDate = new Date(pDate.getTime() + 5.5 * 60 * 60 * 1000);
            const pDateStr = istDate.toISOString().split('T')[0];
            paymentsPool.push({
              bookingId: b.id,
              guestName: b.guestName,
              paymentId: p.id,
              amount: parseFloat(p.amount) || 0,
              date: pDateStr,
              isMatched: false
            });
          }
        }
      }
    }


    const updatedStatements = isUsingMongoDB ? [] : [...readLocalStatements()];
    
    for (let statement of statements) {
      const matchIndex = paymentsPool.findIndex(p => 
        !p.isMatched &&
        p.date === statement.date &&
        Math.abs(p.amount - statement.amount) < 0.01
      );

      if (matchIndex !== -1) {
        const matchedPayment = paymentsPool[matchIndex];
        matchedPayment.isMatched = true;

        statement.isMatched = true;
        statement.matchedBookingId = matchedPayment.bookingId;
        statement.matchedPaymentId = matchedPayment.paymentId;
        statement.description = `${statement.description} [Matched: ${matchedPayment.guestName}]`;

        if (isUsingMongoDB) {
          await StatementRecord.updateOne(
            { id: statement.id },
            { 
              isMatched: true,
              matchedBookingId: statement.matchedBookingId,
              matchedPaymentId: statement.matchedPaymentId,
              description: statement.description
            }
          );
        } else {
          const idx = updatedStatements.findIndex(s => s.id === statement.id);
          if (idx !== -1) {
            updatedStatements[idx] = statement;
          }
        }
      }
    }

    if (!isUsingMongoDB) {
      writeLocalStatements(updatedStatements);
    }

    // Update Excel files on disk so matched rows are styled green
    const uploadedFilesList = isUsingMongoDB ? await UploadedFile.find() : readLocalUploadedFiles();
    for (const fileItem of uploadedFilesList) {
      await updateExcelFileWithMatchedRows(fileItem.filePath);
    }

    const all = isUsingMongoDB ? await StatementRecord.find() : readLocalStatements();
    res.json(all);
  } catch (error) {
    console.error('Error matching statements:', error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running on port ${PORT}`);
});
