# 5ademni.tn

## Face ID Backend

Start the Python Face ID service from the project root with:

```bash
"/opt/anaconda3/bin/python" -m uvicorn face_backend.main:app --host 0.0.0.0 --port 5003 > "/tmp/face_backend.log" 2>&1 &
```

Check that it is running:

```bash
curl http://127.0.0.1:5003/health
```

Expected response:

```json
{ "status": "ok", "backend": "face_recognition+dlib", "threshold": 0.55 }
```

Log file:

```text
/tmp/face_backend.log
```

Stop the service:

```bash
pkill -f 'uvicorn face_backend.main:app --host 0.0.0.0 --port 5003'
```

Verify it stopped:

```bash
curl http://127.0.0.1:5003/health
```

Expected result after stopping: connection refused.

The Symfony web app uses this service on port `5003` for Face ID login and enrollment.

## Testing Accounts:

here are the accounts you can use :
freelancer@gmail.com
client@gmail.com
admin@gmail.com
all these account password is : secret123
