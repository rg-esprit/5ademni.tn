"""
5ademni.tn — Face Recognition Backend  (face_recognition / dlib — no TensorFlow)
POST /enroll              : enroll from pre-captured frames (user_id + frames)
POST /face-login          : identify from pre-captured frames
POST /capture-and-enroll  : open local webcam, capture, enroll (user_id form field)
POST /capture-and-login   : open local webcam, capture, identify
GET  /health              : liveness check
"""

import json
import logging
import os
import time

import cv2
import face_recognition
import mysql.connector
import numpy as np
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.responses import JSONResponse
from typing import List

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="5ademni Face Service", version="2.0.0")

# ── Database ────────────────────────────────────────────────────────────────
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "database": os.getenv("DB_NAME", "appdb"),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASS", ""),
}

# ── Face model config ────────────────────────────────────────────────────────
# face_recognition uses dlib's ResNet-34 → 128-dim unit-hypersphere embeddings.
# Euclidean distance between two descriptors:
#   same person  → typically < 0.50
#   different    → typically > 0.55
THRESHOLD = 0.55          # L2 distance threshold; lower = stricter
DETECTOR_MODEL = "hog"    # "hog" (fast/CPU) or "cnn" (accurate/GPU)


def get_conn():
    return mysql.connector.connect(**DB_CONFIG)


# ── Helpers ──────────────────────────────────────────────────────────────────

def compute_embedding(frame_bgr: np.ndarray) -> np.ndarray | None:
    """
    Return a 128-dim face descriptor from a BGR frame, or None if no face found.
    face_recognition expects RGB; cv2 delivers BGR, so we convert first.
    """
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    locations = face_recognition.face_locations(rgb, model=DETECTOR_MODEL)
    if not locations:
        return None
    encodings = face_recognition.face_encodings(rgb, known_face_locations=locations)
    return encodings[0] if encodings else None


def average_embeddings_from_bgr(frames: List[np.ndarray]) -> np.ndarray:
    """Average 128-dim embeddings from a list of BGR frames. Raises ValueError if none detected."""
    embeddings = []
    for frame in frames:
        emb = compute_embedding(frame)
        if emb is not None:
            embeddings.append(emb)
    if not embeddings:
        raise ValueError(
            "No face detected in any of the provided frames. "
            "Ensure good lighting and face the camera directly."
        )
    return np.mean(np.stack(embeddings), axis=0)


def average_embeddings_from_bytes(frames_bytes: List[bytes]) -> np.ndarray:
    """Decode JPEG bytes to BGR frames, then compute average embedding."""
    frames = []
    for fb in frames_bytes:
        arr = np.frombuffer(fb, np.uint8)
        frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if frame is not None:
            frames.append(frame)
    if not frames:
        raise ValueError("Could not decode any frame bytes.")
    return average_embeddings_from_bgr(frames)


# ── Routes ───────────────────────────────────────────────────────────────────

@app.post("/enroll")
async def enroll(
    user_id: int = Form(...),
    frames: List[UploadFile] = File(...),
):
    """
    Enroll a user's face.
    Expected: user_id (int form field) + frames (1–20 JPEG uploads).
    Stores the averaged 128-dim embedding as JSON in users.face_embedding.
    """
    if not frames:
        return JSONResponse({"success": False, "detail": "No frames received."}, status_code=400)

    logger.info("Enroll request for user_id=%d with %d frames", user_id, len(frames))
    frames_bytes = [await f.read() for f in frames]

    try:
        embedding = average_embeddings_from_bytes(frames_bytes)
    except ValueError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    try:
        conn = get_conn()
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE users SET face_embedding = %s WHERE id = %s",
            (json.dumps(embedding.tolist()), user_id),
        )
        conn.commit()
        affected = cursor.rowcount
        cursor.close()
        conn.close()
    except Exception as exc:
        logger.error("DB error during enroll: %s", exc)
        return JSONResponse({"success": False, "detail": "Database error: " + str(exc)}, status_code=500)

    if affected == 0:
        return JSONResponse({"success": False, "detail": f"User {user_id} not found."}, status_code=404)

    logger.info("Face enrolled for user_id=%d (dim=%d)", user_id, len(embedding))
    return {"success": True, "message": "Face enrolled successfully."}


@app.post("/face-login")
async def face_login(frames: List[UploadFile] = File(...)):
    """
    1:N face identification.
    Expected: frames (3–5 JPEG uploads).
    Returns matched user_id if distance < THRESHOLD, else failure.
    """
    if not frames:
        return JSONResponse({"success": False, "detail": "No frames received."}, status_code=400)

    logger.info("Face-login request with %d frames", len(frames))
    frames_bytes = [await f.read() for f in frames]

    try:
        login_embedding = average_embeddings_from_bytes(frames_bytes)
    except ValueError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    # Load all enrolled embeddings from DB
    try:
        conn = get_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT id, face_embedding FROM users WHERE face_embedding IS NOT NULL")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
    except Exception as exc:
        logger.error("DB error during face-login: %s", exc)
        return JSONResponse({"success": False, "detail": "Database error: " + str(exc)}, status_code=500)

    if not rows:
        return JSONResponse(
            {"success": False, "detail": "No enrolled faces found in the system."},
            status_code=404,
        )

    # Find nearest neighbor (L2 distance)
    best_id, best_dist = None, float("inf")
    for user_id, emb_json in rows:
        try:
            stored = np.array(json.loads(emb_json))
            dist = float(np.linalg.norm(login_embedding - stored))
            if dist < best_dist:
                best_dist = dist
                best_id = user_id
        except Exception as exc:
            logger.warning("Skipping user %s — bad embedding: %s", user_id, exc)

    logger.info("Best match: user_id=%s, distance=%.4f (threshold=%.2f)", best_id, best_dist, THRESHOLD)

    if best_id is not None and best_dist < THRESHOLD:
        return {"success": True, "user_id": best_id, "distance": round(best_dist, 4)}

    return JSONResponse(
        {
            "success": False,
            "detail": "Face not recognized. Please try again or use password login.",
            "best_distance": round(best_dist, 4) if best_dist != float("inf") else None,
        },
        status_code=401,
    )


@app.get("/health")
def health():
    return {"status": "ok", "backend": "face_recognition+dlib", "threshold": THRESHOLD}


# ── Camera-side capture endpoints (Python opens the webcam) ──────────────────

def capture_frames_cv2(count: int, delay_ms: int = 200) -> List[np.ndarray]:
    """
    Open the default webcam, capture `count` BGR frames with `delay_ms` between
    each, and return them as a list of BGR numpy arrays.
    Raises RuntimeError if the camera cannot be opened.
    """
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        import platform
        if platform.system() == "Darwin":
            raise RuntimeError(
                "Cannot open camera. On macOS, grant camera permission to Terminal: "
                "System Settings → Privacy & Security → Camera → enable Terminal (or iTerm2)."
            )
        raise RuntimeError("Cannot open webcam (camera index 0). Ensure no other app is using the camera.")
    # Warm-up — discard the first few frames so auto-exposure settles
    for _ in range(5):
        cap.read()
        time.sleep(0.05)

    frames = []
    try:
        for _ in range(count):
            ret, frame = cap.read()
            if ret and frame is not None:
                frames.append(frame)   # keep as BGR; compute_embedding converts
            time.sleep(delay_ms / 1000.0)
    finally:
        cap.release()

    if not frames:
        raise RuntimeError("Webcam returned no frames.")
    return frames


@app.post("/capture-and-enroll")
async def capture_and_enroll(
    user_id: int = Form(...),
    frame_count: int = Form(default=12),
):
    """
    Open the local webcam, capture frames, compute the average Facenet
    embedding, and store it in users.face_embedding for the given user_id.
    Intended to be called from JavaFX; Python owns the camera.
    """
    logger.info("capture-and-enroll: user_id=%d, frame_count=%d", user_id, frame_count)
    try:
        raw_frames = capture_frames_cv2(frame_count)
    except RuntimeError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    try:
        embedding = average_embeddings_from_bgr(raw_frames)
    except ValueError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    try:
        conn = get_conn()
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE users SET face_embedding = %s WHERE id = %s",
            (json.dumps(embedding.tolist()), user_id),
        )
        conn.commit()
        affected = cursor.rowcount
        cursor.close()
        conn.close()
    except Exception as exc:
        logger.error("DB error during capture-and-enroll: %s", exc)
        return JSONResponse({"success": False, "detail": "Database error: " + str(exc)}, status_code=500)

    if affected == 0:
        return JSONResponse({"success": False, "detail": f"User {user_id} not found."}, status_code=404)

    logger.info("Face enrolled via webcam for user_id=%d", user_id)
    return {"success": True, "message": "Face enrolled successfully."}


@app.post("/capture-and-login")
async def capture_and_login(frame_count: int = Form(default=5)):
    """
    Open the local webcam, capture frames, compute the average Facenet
    embedding, compare against all enrolled users, return matched user_id.
    """
    logger.info("capture-and-login: frame_count=%d", frame_count)
    try:
        raw_frames = capture_frames_cv2(frame_count)
    except RuntimeError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    try:
        login_embedding = average_embeddings_from_bgr(raw_frames)
    except ValueError as exc:
        return JSONResponse({"success": False, "detail": str(exc)}, status_code=400)

    # Load all enrolled users
    try:
        conn = get_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT id, face_embedding FROM users WHERE face_embedding IS NOT NULL")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
    except Exception as exc:
        logger.error("DB error during capture-and-login: %s", exc)
        return JSONResponse({"success": False, "detail": "Database error: " + str(exc)}, status_code=500)

    if not rows:
        return JSONResponse(
            {"success": False, "detail": "No enrolled faces found in the system."},
            status_code=404,
        )

    best_id, best_dist = None, float("inf")
    for uid, emb_json in rows:
        try:
            stored = np.array(json.loads(emb_json))
            dist = float(np.linalg.norm(login_embedding - stored))
            if dist < best_dist:
                best_dist = dist
                best_id = uid
        except Exception as exc:
            logger.warning("Skipping user %s — bad embedding: %s", uid, exc)

    logger.info("Best match: user_id=%s, distance=%.4f (threshold=%.2f)", best_id, best_dist, THRESHOLD)

    if best_id is not None and best_dist < THRESHOLD:
        return {"success": True, "user_id": best_id, "distance": round(best_dist, 4)}

    return JSONResponse(
        {
            "success": False,
            "detail": "Face not recognized. Try again or use password login.",
            "best_distance": round(best_dist, 4) if best_dist != float("inf") else None,
        },
        status_code=401,
    )
