const tls = require('tls');
const fs = require('fs');

/**
 * Normalizes date string into YYYY-MM-DD format.
 */
function normalizeDate(dateStr) {
  if (!dateStr) return new Date().toISOString().split('T')[0];
  try {
    const cleanStr = dateStr.replace(/(st|nd|rd|th)/gi, '').trim();
    const parsed = new Date(cleanStr);
    if (!isNaN(parsed.getTime())) {
      return parsed.toISOString().split('T')[0];
    }
  } catch (e) {}
  
  // Fallback regex matching e.g. 2026-08-04 or 04-08-2026
  const match = dateStr.match(/(\d{4})[-/](\d{1,2})[-/](\d{1,2})/) || dateStr.match(/(\d{1,2})[-/](\d{1,2})[-/](\d{4})/);
  if (match) {
    if (match[1].length === 4) return `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`;
    return `${match[3]}-${match[2].padStart(2, '0')}-${match[1].padStart(2, '0')}`;
  }
  return new Date().toISOString().split('T')[0];
}

/**
 * Parses raw text/HTML email content into structured OTA booking or cancellation details.
 */
function parseEmailContent(subject = "", body = "") {
  const combined = `${subject}\n${body}`.replace(/\r\n/g, '\n');
  const lowerSubject = subject.toLowerCase();
  const lowerCombined = combined.toLowerCase();

  // 1. Detect Action Type
  const isCancellation = /cancel|cancellation|cancelled|canceled/i.test(lowerSubject) || 
                         /booking\s*cancelled|reservation\s*cancelled|booking\s*cancellation/i.test(lowerCombined);
  
  const isNewBooking = /booking|reservation|confirmation|confirmed/i.test(lowerSubject) || 
                       /new\s*booking|booking\s*confirmation|reservation\s*confirmed/i.test(lowerCombined);

  if (!isCancellation && !isNewBooking) {
    return null; // Not an OTA reservation email
  }

  // 2. Detect Platform
  let platform = "MMT";
  if (/goibibo|gb\d+/i.test(combined)) platform = "Goibibo";
  else if (/booking\.com/i.test(combined)) platform = "Booking.com";
  else if (/agoda/i.test(combined)) platform = "Agoda";
  else if (/yatra/i.test(combined)) platform = "Yatra";
  else if (/cleartrip/i.test(combined)) platform = "Cleartrip";
  else if (/makemytrip|mmt|nh\d+/i.test(combined)) platform = "MMT";

  // 3. Extract Booking Reference ID
  let otaBookingId = "";
  const idMatch = combined.match(/(?:booking|reservation|ref|confirmation)\s*(?:id|number|no\.?|code)?:?\s*#?\s*([A-Z0-9_-]{5,25})/i) ||
                  combined.match(/(NH[0-9A-Z]+|GB[0-9A-Z]+|\d{7,15})/i);
  if (idMatch) {
    otaBookingId = idMatch[1].trim();
  }

  // 4. Extract Guest Name
  let guestName = "OTA Guest";
  const guestMatch = combined.match(/(?:primary\s*guest|lead\s*guest|guest\s*name|guest|customer\s*name):?\s*([A-Za-z\s.]{2,35})/i) ||
                     combined.match(/name:?\s*([A-Za-z\s.]{2,35})/i);
  if (guestMatch) {
    const rawName = guestMatch[1].trim().split('\n')[0].replace(/[^a-zA-Z\s.]/g, '').trim();
    if (rawName.length > 1 && !/booking|hotel|check|room|total|amount/i.test(rawName)) {
      guestName = rawName;
    }
  }

  // 5. Extract Check-in & Check-out Dates
  let checkInDate = new Date().toISOString().split('T')[0];
  let checkOutDate = "";

  const checkInMatch = combined.match(/(?:check-?in|arrival|from|date\s*of\s*arrival):?\s*([A-Za-z0-9\s,/-]{6,25})/i);
  if (checkInMatch) {
    checkInDate = normalizeDate(checkInMatch[1].trim().split('\n')[0]);
  }

  const checkOutMatch = combined.match(/(?:check-?out|departure|to|date\s*of\s*departure):?\s*([A-Za-z0-9\s,/-]{6,25})/i);
  if (checkOutMatch) {
    checkOutDate = normalizeDate(checkOutMatch[1].trim().split('\n')[0]);
  }

  // 6. Extract Amount / Payable Rate
  let billAmount = 0.0;
  const amountMatch = combined.match(/(?:payable\s*to\s*hotel|net\s*payable|total\s*(?:amount|price|payable|rate)|booking\s*amount):?\s*₹?\s*([\d,.]+)/i) ||
                      combined.match(/₹\s*([\d,.]+)/);
  if (amountMatch) {
    const parsedAmt = parseFloat(amountMatch[1].replace(/,/g, ''));
    if (!isNaN(parsedAmt) && parsedAmt > 0) {
      billAmount = parsedAmt;
    }
  }

  return {
    action: isCancellation ? "CANCEL" : "NEW",
    platform,
    otaBookingId,
    guestName,
    checkInDate,
    checkOutDate,
    billAmount,
    rawSubject: subject
  };
}

/**
 * IMAP Engine to connect via TLS to email server and fetch unread emails.
 */
function fetchEmailsFromIMAP(config) {
  return new Promise((resolve, reject) => {
    const { host = "imap.gmail.com", port = 993, email, appPassword } = config;
    if (!email || !appPassword) {
      return reject(new Error("Missing email or appPassword configuration."));
    }

    const cleanPass = appPassword.replace(/\s+/g, '');
    const client = tls.connect(port, host, { rejectUnauthorized: false }, () => {
      console.log(`[IMAP] Connected to ${host}:${port}`);
    });

    let step = 0;
    let rawBuffer = "";
    const fetchedEmails = [];
    let currentEmail = null;

    client.setEncoding('utf8');

    const sendCommand = (cmd) => {
      client.write(cmd + "\r\n");
    };

    client.on('data', (data) => {
      rawBuffer += data;
      const lines = rawBuffer.split("\r\n");
      rawBuffer = lines.pop(); // Keep partial line in buffer

      for (let line of lines) {
        if (step === 0 && line.includes("* OK")) {
          step = 1;
          sendCommand(`A1 LOGIN "${email}" "${cleanPass}"`);
        } else if (step === 1 && line.includes("A1 OK")) {
          step = 2;
          sendCommand(`A2 SELECT INBOX`);
        } else if (step === 2 && line.includes("A2 OK")) {
          step = 3;
          sendCommand(`A3 SEARCH UNSEEN`);
        } else if (step === 3 && line.startsWith("* SEARCH")) {
          const ids = line.replace("* SEARCH", "").trim().split(/\s+/).filter(Boolean);
          if (ids.length === 0) {
            sendCommand(`A3_2 SEARCH ALL`);
          } else {
            step = 4;
            const targetIds = ids.slice(-20).join(","); // Fetch last 20 emails
            sendCommand(`A4 FETCH ${targetIds} (BODY[HEADER.FIELDS (SUBJECT FROM DATE)] BODY[TEXT])`);
          }
        } else if (step === 3 && line.startsWith("A3_2 SEARCH")) {
          const ids = line.replace("* SEARCH", "").trim().split(/\s+/).filter(Boolean);
          if (ids.length === 0) {
            step = 5;
            sendCommand(`A5 LOGOUT`);
          } else {
            step = 4;
            const targetIds = ids.slice(-20).join(",");
            sendCommand(`A4 FETCH ${targetIds} (BODY[HEADER.FIELDS (SUBJECT FROM DATE)] BODY[TEXT])`);
          }
        } else if (step === 4) {
          if (line.startsWith("* ") && line.includes("FETCH")) {
            if (currentEmail) fetchedEmails.push(currentEmail);
            currentEmail = { subject: "", body: "" };
          } else if (line.toLowerCase().startsWith("subject:")) {
            if (currentEmail) currentEmail.subject = line.substring(8).trim();
          } else if (currentEmail) {
            currentEmail.body += line + "\n";
          }

          if (line.includes("A4 OK")) {
            if (currentEmail) fetchedEmails.push(currentEmail);
            step = 5;
            sendCommand(`A5 LOGOUT`);
          }
        } else if (step === 5 && line.includes("A5 OK")) {
          client.end();
        }
      }
    });

    client.on('end', () => {
      resolve(fetchedEmails);
    });

    client.on('error', (err) => {
      reject(err);
    });

    // Timeout safety
    setTimeout(() => {
      try { client.end(); } catch (e) {}
      resolve(fetchedEmails);
    }, 20000);
  });
}

module.exports = {
  parseEmailContent,
  fetchEmailsFromIMAP
};
