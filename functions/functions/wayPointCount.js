"use strict";

const admin = require('firebase-admin');
const functions = require("firebase-functions");
const db = admin.firestore();

// Keeps track of the length of the "waypoints" child list in a separate property.
exports.wayPointCount = functions.firestore.document("/routes/{routeId}/waypoints/{waypointId}").onWrite(
  async (change, context) => {
    const routeId = context.params.routeId;
    const routeRef = db.collection('routes').doc(routeId);
    const FieldValue = require('firebase-admin').firestore.FieldValue;

    if (!change.before.exists) {
      routeRef.update({wayPointCount: FieldValue.increment(1)});
    } else if (!change.after.exists) {
      routeRef.update({wayPointCount: FieldValue.increment(-1)});
    }
    return 0;
  });
