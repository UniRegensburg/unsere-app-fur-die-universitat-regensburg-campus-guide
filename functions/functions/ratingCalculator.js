"use strict";

const admin = require('firebase-admin');
const functions = require("firebase-functions");
const db = admin.firestore();

const average = (array) => array.reduce((a, b) => a + b) / array.length;

// Keeps track of the length of the "waypoints" child list in a separate property.
exports.ratingCalculator = functions.firestore.document("/routes/{routeId}").onUpdate(
    async (change, context) => {
        if (change.before.exists) {
            const routeId = context.params.routeId;
            const routeRef = db.collection('routes').doc(routeId);
            const ratingCol = db.collection('ratings');
            const FieldPath = admin.firestore.FieldPath;

            const afterData = change.after.data();
            const previousData = change.before.data();

            const afterArray = afterData.rating;
            const previousArray = previousData.rating;

            if (afterArray.length !== previousArray.length) {
                return await ratingCol.where(FieldPath.documentId(), "in", afterArray).get().then((snapshots) => {
                    const ratingList = [];
                    snapshots.forEach((doc) => {
                        const rating = doc.data().ratingValue;
                        ratingList.push(rating);
                    });
                    const averageRating = average(ratingList);
                    const roundedRating = Math.round(averageRating);
                    routeRef.update({currentRating: roundedRating});
                    return 0;
                });
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    });
