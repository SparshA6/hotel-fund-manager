const tls = require('tls');
const fs = require('fs');

/**
 * Decodes quoted-printable strings.
 */
function decodeQuotedPrintable(str = "") {
  return str
    .replace(/=\r?\n/g, '')
    .replace(/=([0-9A-F]{2})/gi, (match, hex) => String.fromCharCode(parseInt(hex, 16)));
}

/**
 * Cleans HTML content to plain readable text with newlines.
 */
function cleanHtmlToText(html = "") {
  if (!html) return "";
  let text = decodeQuotedPrintable(html);

  // Remove scripts & styles
  text = text.replace(/<style[^>]*>[\s\S]*?<\/style>/gi, ' ');
  text = text.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ');

  // Structural HTML tags to newlines
  text = text.replace(/<\/(td|tr|div|p|h[1-6]|li|br)>/gi, '\n');
  text = text.replace(/<br\s*\/?>/gi, '\n');

  // Strip all other HTML tags
  text = text.replace(/<[^>]+>/g, ' ');

  // Decode common HTML entities
  text = text.replace(/&nbsp;/gi, ' ')
             .replace(/&amp;/gi, '&')
             .replace(/&quot;/gi, '"')
             .replace(/&#8377;/gi, '₹')
             .replace(/&inr;/gi, '₹')
             .replace(/&lt;/gi, '<')
             .replace(/&gt;/gi, '>');

  // Clean up whitespace & blank lines
  text = text.replace(/[ \t]+/g, ' ');
  text = text.replace(/\n\s*\n/g, '\n');
  return text;
}

/**
 * Normalizes date strings to YYYY-MM-DD format.
 */
function normalizeDate(dateStr) {
  if (!dateStr) return new Date().toISOString().split('T')[0];
  try {
    const cleanStr = dateStr.replace(/(st|nd|rd|th)/gi, '').replace(/,/g, '').trim();
    const parsed = new Date(cleanStr);
    if (!isNaN(parsed.getTime())) {
      const year = parsed.getFullYear();
      if (year >= 2020 && year <= 2035) {
        const month = String(parsed.getMonth() + 1).padStart(2, '0');
        const day = String(parsed.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      }
    }
  } catch (e) {}

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
function parseEmailContent(subject = "", rawBody = "") {
  const cleanBodyText = cleanHtmlToText(rawBody);
  const combined = `${subject}\n${cleanBodyText}`.replace(/\r\n/g, '\n');
  const lowerSubject = subject.toLowerCase();
  const lowerCombined = combined.toLowerCase();

  // 1. Detect Action Type
  const isCancellation = /cancel|cancellation|cancelled|canceled/i.test(lowerSubject) || 
                         /booking\s*cancelled|reservation\s*cancelled|booking\s*cancellation/i.test(lowerCombined);
  
  const isNewBooking = /booking|reservation|confirmation|confirmed/i.test(lowerSubject) || 
                       /new\s*booking|booking\s*confirmation|reservation\s*confirmed/i.test(lowerCombined);

  if (!isCancellation && !isNewBooking) {
    return null;
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
  const idMatch = combined.match(/(NH[0-9A-Z]{8,18})/i) ||
                  combined.match(/(GB[0-9A-Z]{8,18})/i) ||
                  combined.match(/(?:booking|reservation|ref|confirmation)\s*(?:id|number|no\.?|code)?:?\s*#?\s*([A-Z0-9_-]{5,25})/i);
  if (idMatch) {
    otaBookingId = idMatch[1].trim();
  }

  // 4. Extract Guest Name
  let guestName = "OTA Guest";
  const guestMatch = combined.match(/(?:primary\s*guest|lead\s*guest|guest\s*name|guest|customer\s*name|booker\s*name):?\s*([A-Za-z\s.]{2,35})/i) ||
                     combined.match(/name:?\s*([A-Za-z\s.]{2,35})/i);
  if (guestMatch) {
    const rawName = guestMatch[1].trim().split('\n')[0].replace(/[^a-zA-Z\s.]/g, '').trim();
    const forbidden = /booking|hotel|check|room|total|amount|night|adult|child|status|payment|payable|policy|cancellation/i;
    if (rawName.length > 1 && !forbidden.test(rawName)) {
      guestName = rawName;
    }
  }

  // 5. Extract Room Category / Type
  let roomCategory = "Deluxe";
  const roomMatch = combined.match(/(?:room\s*(?:type|category|name)|category):?\s*([A-Za-z0-9\s-]{3,35})/i);
  if (roomMatch) {
    const rawCategory = roomMatch[1].trim().split('\n')[0].trim();
    if (rawCategory.length > 2 && !/booking|hotel|check|total|amount/i.test(rawCategory)) {
      roomCategory = rawCategory;
    }
  } else {
    if (/dorm/i.test(combined)) roomCategory = "Dorm";
    else if (/suite/i.test(combined)) roomCategory = "Suite";
    else if (/executive/i.test(combined)) roomCategory = "Executive";
    else if (/super\s*deluxe/i.test(combined)) roomCategory = "Super Deluxe";
    else if (/standard/i.test(combined)) roomCategory = "Standard";
    else if (/deluxe/i.test(combined)) roomCategory = "Deluxe";
  }

  // 6. Extract Check-in & Check-out Dates
  let checkInDate = new Date().toISOString().split('T')[0];
  let checkOutDate = "";

  const checkInMatch = combined.match(/(?:check-?in|arrival|from|date\s*of\s*arrival):?\s*([A-Za-z0-9\s,/-]{6,30})/i);
  if (checkInMatch) {
    checkInDate = normalizeDate(checkInMatch[1].trim().split('\n')[0]);
  }

  const checkOutMatch = combined.match(/(?:check-?out|departure|to|date\s*of\s*departure):?\s*([A-Za-z0-9\s,/-]{6,30})/i);
  if (checkOutMatch) {
    checkOutDate = normalizeDate(checkOutMatch[1].trim().split('\n')[0]);
  }

  // 7. Extract Amount / Payable Rate
  let billAmount = 0.0;
  const amountMatch = combined.match(/(?:payable\s*to\s*hotel|net\s*payable|total\s*(?:amount|price|payable|rate)|booking\s*amount|rate):?\s*₹?\s*([\d,.]+)/i) ||
                      combined.match(/₹\s*([\d,.]+)/) ||
                      combined.match(/INR\s*([\d,.]+)/i);
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
    roomCategory,
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
