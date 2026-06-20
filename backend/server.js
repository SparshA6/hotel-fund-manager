const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

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

// Connect to MongoDB Atlas
const maskedURI = MONGODB_URI.replace(/:([^:@]+)@/, ':***@');
console.log('Attempting to connect to MongoDB URI:', maskedURI);

mongoose.connect(MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB successfully!');
    isUsingMongoDB = true;
  })
  .catch((err) => {
    console.warn('MongoDB connection failed. Falling back to local JSON file storage.');
    console.warn('Error details:', err.message);
    isUsingMongoDB = false;
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

// REST API Endpoints

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

    if (isUsingMongoDB) {
      const result = await Booking.findOneAndDelete({ id: bookingId });
      if (!result) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      res.json({ message: 'Booking deleted successfully', id: bookingId });
    } else {
      const bookings = readLocalBookings();
      const filtered = bookings.filter(b => b.id !== bookingId);
      if (bookings.length === filtered.length) {
        return res.status(404).json({ error: 'Booking not found' });
      }
      writeLocalBookings(filtered);
      res.json({ message: 'Booking deleted successfully', id: bookingId });
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

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running on port ${PORT}`);
});
