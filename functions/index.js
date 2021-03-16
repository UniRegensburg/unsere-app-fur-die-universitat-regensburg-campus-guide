"use strict";

// Init firebase functions tools
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Get functions from different files
const commentCount = require("./functions/commentCount");
const wayPointCount = require("./functions/wayPointCount");
const ratingCalculator = require("./functions/ratingCalculator");

// Export functions to firebase
exports.commentCount = commentCount.commentCount;
exports.wayPointCount = wayPointCount.wayPointCount;
exports.ratingCalculator = ratingCalculator.ratingCalculator;
