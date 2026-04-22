const FACE_CAPTURE_DELAY_MS = 180;
const FACE_CAPTURE_QUALITY = 0.92;
const FACE_FORM_SELECTOR = '[data-face-camera-form]';

function initializeFaceCameraForms() {
    document.querySelectorAll(FACE_FORM_SELECTOR).forEach((form) => {
        if (form.dataset.faceCameraBound === 'true') {
            return;
        }

        form.dataset.faceCameraBound = 'true';
        bindFaceCameraForm(form);
    });
}

function bindFaceCameraForm(form) {
    const video = form.querySelector('[data-face-camera-video]');
    const framesInput = form.querySelector('[data-face-camera-frames]');
    const status = form.querySelector('[data-face-camera-status]');
    const toggleButton = form.querySelector('[data-face-camera-toggle]');
    const submitButton = form.querySelector('[data-face-camera-submit]');

    if (!video || !framesInput || !status || !toggleButton || !submitButton) {
        return;
    }

    const captureCount = Math.max(Number.parseInt(form.dataset.faceCameraCount || '4', 10) || 4, 1);
    const messages = {
        initial: status.textContent.replace(/\s+/g, ' ').trim(),
        requesting: 'Requesting camera access...',
        ready: 'Camera ready. Keep your face centered and continue.',
        capturing: 'Capturing Face ID frames...',
        submitting: 'Submitting Face ID scan...',
        denied: 'Camera access is required for Face ID.',
        unsupported: 'This browser does not support live camera capture.',
        captureFailed: 'We could not capture your Face ID scan. Please try again.',
    };

    let stream = null;
    let busy = false;

    const setStatus = (message, isError = false) => {
        status.textContent = message;
        status.dataset.error = isError ? 'true' : 'false';
    };

    const updateButtons = () => {
        toggleButton.textContent = stream ? 'Stop Camera' : 'Start Camera';
        toggleButton.setAttribute('aria-pressed', stream ? 'true' : 'false');
        toggleButton.disabled = busy;
        submitButton.disabled = busy;
    };

    const releaseCamera = () => {
        if (!stream) {
            return;
        }

        stream.getTracks().forEach((track) => track.stop());
        stream = null;
        video.srcObject = null;
    };

    const stopCamera = () => {
        releaseCamera();
        setStatus(messages.initial);
        updateButtons();
    };

    const startCamera = async () => {
        if (!navigator.mediaDevices?.getUserMedia || typeof DataTransfer === 'undefined') {
            throw new Error('unsupported');
        }

        if (stream) {
            return stream;
        }

        setStatus(messages.requesting);

        try {
            stream = await navigator.mediaDevices.getUserMedia({
                audio: false,
                video: {
                    facingMode: { ideal: 'user' },
                    width: { ideal: 640 },
                    height: { ideal: 480 },
                },
            });
            video.srcObject = stream;
            await video.play();
            await waitForVideoFrame(video);
            setStatus(messages.ready);
            updateButtons();

            return stream;
        } catch (error) {
            releaseCamera();
            updateButtons();
            throw error;
        }
    };

    toggleButton.addEventListener('click', async () => {
        if (busy) {
            return;
        }

        if (stream) {
            stopCamera();

            return;
        }

        try {
            await startCamera();
        } catch (error) {
            setStatus(getErrorMessage(error, messages), true);
        }
    });

    form.addEventListener('submit', async (event) => {
        if (busy) {
            event.preventDefault();

            return;
        }

        event.preventDefault();
        busy = true;
        updateButtons();

        try {
            await startCamera();
            setStatus(messages.capturing);
            framesInput.files = buildFileList(await captureFrames(video, captureCount));
            releaseCamera();
            setStatus(messages.submitting);
            updateButtons();
            HTMLFormElement.prototype.submit.call(form);
        } catch (error) {
            busy = false;
            updateButtons();
            setStatus(getErrorMessage(error, messages), true);
        }
    });

    window.addEventListener('pagehide', releaseCamera);
    updateButtons();
}

async function captureFrames(video, captureCount) {
    const width = video.videoWidth;
    const height = video.videoHeight;

    if (width < 1 || height < 1) {
        throw new Error('camera_unavailable');
    }

    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    if (!context) {
        throw new Error('camera_unavailable');
    }

    canvas.width = width;
    canvas.height = height;

    const files = [];

    for (let index = 0; index < captureCount; index += 1) {
        context.drawImage(video, 0, 0, width, height);

        const blob = await new Promise((resolve) => {
            canvas.toBlob(resolve, 'image/jpeg', FACE_CAPTURE_QUALITY);
        });

        if (!blob) {
            throw new Error('camera_unavailable');
        }

        files.push(new File([blob], `face-frame-${Date.now()}-${index + 1}.jpg`, {
            type: 'image/jpeg',
        }));

        if (index < captureCount - 1) {
            await sleep(FACE_CAPTURE_DELAY_MS);
        }
    }

    return files;
}

function buildFileList(files) {
    const dataTransfer = new DataTransfer();

    files.forEach((file) => dataTransfer.items.add(file));

    return dataTransfer.files;
}

function getErrorMessage(error, messages) {
    if (error.message === 'unsupported') {
        return messages.unsupported;
    }

    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
        return messages.denied;
    }

    return messages.captureFailed;
}

async function waitForVideoFrame(video) {
    for (let attempt = 0; attempt < 20; attempt += 1) {
        if (video.videoWidth > 0 && video.videoHeight > 0) {
            return;
        }

        await sleep(100);
    }

    throw new Error('camera_unavailable');
}

function sleep(milliseconds) {
    return new Promise((resolve) => {
        window.setTimeout(resolve, milliseconds);
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeFaceCameraForms);
} else {
    initializeFaceCameraForms();
}
