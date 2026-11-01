# SignBridge-Java — Full Trainable Prototype

A Java 17 desktop prototype with webcam capture, a user-trainable static-gesture classifier, sentence composition, speech output, and SQLite history.

## What is actually implemented
- Swing desktop UI with OpenCV webcam preview.
- Capture labelled examples from the webcam into `dataset/<LABEL>/`.
- Train an OpenCV K-Nearest Neighbours (KNN) classifier from the captured examples using HOG image features.
- Live static-image gesture prediction with a confidence/distance guard.
- Phrase builder, undo, clear, copy and system text-to-speech.
- SQLite history.
- Starter labels: HELLO, THANK YOU, YES, NO, HELP, PLEASE, STOP, WATER.
- Dataset count and model status in the app.

## Important limitations
This is a **trainable static-gesture prototype**, not a certified or complete Indian Sign Language translator. It classifies still hand poses from the examples you record. It does not yet understand dynamic signs, facial grammar, two-handed signs, sentence grammar, or all ISL. HOG+KNN is a classical baseline and may be sensitive to lighting, background, camera position, and signer differences. Treat low-confidence output as unknown and test with multiple consenting signers.

The project does not ship a pretrained ISL model or claim pre-measured accuracy. You create your own labelled dataset in the app and train locally. Do not label arbitrary ASL examples as ISL.

## Requirements
- JDK 17+
- Maven 3.9+
- Webcam
- Windows, macOS or Linux
- Linux speech: install `espeak` if speech output is needed. macOS uses `say`; Windows uses PowerShell System.Speech.

## Run
```bash
mvn clean compile
mvn exec:java
```

## How to use
1. Start the app and click **Start Camera**.
2. Keep one hand inside the camera frame, centered, with a plain background and consistent lighting.
3. Select a label from **Capture label**, then click **Capture 30**. Keep the same sign pose while it captures a short burst; vary hand position slightly between separate capture sessions.
4. Repeat for each class. For a meaningful first experiment, record at least 50–100 examples per label, with varied but realistic samples. Include an `OTHER` class for non-target/uncertain poses.
5. Click **Train KNN Model**. Training requires at least two labels and at least two samples per label.
6. Switch to **Live Predict** and hold a static pose. Only add a predicted label to the sentence when the displayed prediction is stable and plausible.
7. Click **Save Phrase** to store the translation in SQLite.

## Dataset and model files
- `dataset/<LABEL>/*.png`: captured labelled images.
- `models/signbridge-knn.yml`: trained OpenCV KNN model.
- `models/labels.txt`: numeric class-to-label mapping.
- `data/signbridge.db`: translation history.

## Model approach
1. Webcam frame is cropped to a centered square.
2. Image is converted to grayscale, resized to 64×64, and histogram-equalized.
3. HOG descriptor extracts shape/edge features.
4. OpenCV KNN classifies the descriptor against recorded examples.

This is a practical baseline for a Java-only demo. A stronger research version should replace the image-based classifier with hand landmarks and a sequence model trained on a curated ISL dataset, then evaluate signer-independent performance.

## Troubleshooting
- Camera unavailable: close Zoom/Teams/browser tabs using the webcam; try changing camera index in `CameraService`.
- Native OpenCV load issue: use a supported 64-bit JDK and rerun Maven.
- Weak predictions: capture more varied examples, improve lighting, use a plain background, and add an `OTHER` label.
- Speech unavailable: install/configure the platform speech engine.
