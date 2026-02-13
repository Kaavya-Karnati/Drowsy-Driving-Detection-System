from ultralytics import YOLO
import cv2

# Load your trained model
model = YOLO('/Users/nrpatel370/Desktop/v1-yolov8n/detect/train/weights/best.pt')


import cv2

# Use AVFoundation backend on macOS
video_capture = cv2.VideoCapture(0, cv2.CAP_AVFOUNDATION)

if not video_capture.isOpened():
    print("Error: Could not open camera")
    exit()

while True:
    ret, frame = video_capture.read()
    if not ret:
        print("Failed to grab frame")
        break

    # Run YOLO inference
    results = model(frame)

    # Draw bounding boxes on frame
    detected_frame = results[0].plot()

    # Show the DETECTED frame
    cv2.imshow('YOLO', detected_frame)

    # ESC key to exit
    if cv2.waitKey(1) & 0xFF == 27:
        break

video_capture.release()
cv2.destroyAllWindows()
