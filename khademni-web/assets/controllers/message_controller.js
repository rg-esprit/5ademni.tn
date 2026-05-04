import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['content', 'attachment', 'emojiToggle', 'emojiPicker', 'attachPhoto', 
                      'attachFile', 'recordAudioButton', 'composerSendButton', 'scheduleButton',
                      'startAudioCall', 'startVideoCall', 'summarizeChat', 'openScheduleMeeting',
                      'chatSearchInput', 'threadMessages', 'composerAction', 'audioDuration',
                      'attachmentPreview', 'fileInput'];
    
    static values = {
        conversationId: Number,
        userId: Number,
    }

    connect() {
        this.setupEventListeners();
        this.recordingState = {
            isRecording: false,
            mediaRecorder: null,
            chunks: [],
        };
    }

    // ============ EMOJI PICKER ============
    toggleEmojiPicker(e) {
        e.preventDefault();
        this.emojiPickerTarget.classList.toggle('visible');
    }

    insertEmoji(e) {
        e.preventDefault();
        if (e.target.tagName === 'BUTTON') {
            const emoji = e.target.textContent;
            const textarea = this.contentTarget;
            const start = textarea.selectionStart;
            const end = textarea.selectionEnd;
            textarea.value = textarea.value.substring(0, start) + emoji + textarea.value.substring(end);
            textarea.focus();
            textarea.setSelectionRange(start + emoji.length, start + emoji.length);
            this.emojiPickerTarget.classList.remove('visible');
        }
    }

    // ============ FILE ATTACHMENT ============
    attachPhoto(e) {
        e.preventDefault();
        this.fileInputTarget.accept = 'image/*';
        this.fileInputTarget.click();
    }

    attachFile(e) {
        e.preventDefault();
        this.fileInputTarget.accept = 'image/*,audio/*,application/pdf,.doc,.docx,.txt,.xls,.xlsx';
        this.fileInputTarget.click();
    }

    handleFileSelect(e) {
        const file = e.target.files[0];
        if (file) {
            const maxSize = 50 * 1024 * 1024; // 50MB
            if (file.size > maxSize) {
                alert('Le fichier est trop volumineux. Taille maximale: 50MB');
                return;
            }

            const fileName = file.name;
            const fileSize = (file.size / 1024 / 1024).toFixed(2);
            this.attachmentPreviewTarget.textContent = `Pièce jointe: ${fileName} (${fileSize}MB)`;
        }
    }

    // ============ AUDIO RECORDING ============
    async recordAudio(e) {
        e.preventDefault();

        if (this.recordingState.isRecording) {
            this.stopRecording();
            return;
        }

        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream);
            this.recordingState.mediaRecorder = mediaRecorder;
            this.recordingState.chunks = [];
            this.recordingState.isRecording = true;
            this.recordAudioButtonTarget.textContent = '⏹️ Arrêter';
            this.recordAudioButtonTarget.classList.add('recording');

            mediaRecorder.ondataavailable = (e) => {
                this.recordingState.chunks.push(e.data);
            };

            mediaRecorder.onstop = () => {
                const blob = new Blob(this.recordingState.chunks, { type: 'audio/webm' });
                this.handleAudioBlob(blob);
            };

            mediaRecorder.start();
        } catch (err) {
            console.error('Erreur d\'accès au microphone:', err);
            alert('Impossible d\'accéder au microphone. Vérifiez les permissions.');
        }
    }

    stopRecording() {
        if (this.recordingState.mediaRecorder) {
            this.recordingState.mediaRecorder.stop();
            this.recordingState.isRecording = false;
            this.recordAudioButtonTarget.textContent = '🎤';
            this.recordAudioButtonTarget.classList.remove('recording');
            
            // Arrêter le flux audio
            this.recordingState.mediaRecorder.stream.getTracks().forEach(track => track.stop());
        }
    }

    handleAudioBlob(blob) {
        const file = new File([blob], `audio_${Date.now()}.webm`, { type: 'audio/webm' });
        
        // Créer un FormData et ajouter le fichier audio
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        
        const fileInput = this.element.querySelector('input[name="attachment"]');
        if (fileInput) {
            fileInput.files = dataTransfer.files;
        }

        // Calculer la durée de l'audio
        const audio = new Audio();
        audio.onloadedmetadata = () => {
            const duration = Math.round(audio.duration);
            this.audioDurationTarget.value = `${Math.floor(duration / 60)}:${String(duration % 60).padStart(2, '0')}`;
            this.attachmentPreviewTarget.textContent = `Message vocal enregistré (${this.audioDurationTarget.value})`;
        };
        audio.src = URL.createObjectURL(blob);
    }

    // ============ CALLS ============
    async startAudioCall(e) {
        e.preventDefault();
        await this.initiateCall('audio');
    }

    async startVideoCall(e) {
        e.preventDefault();
        await this.initiateCall('video');
    }

    async initiateCall(type) {
        try {
            const response = await fetch(`/messages/${this.conversationIdValue}/call`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    type: type,
                }),
            });

            const data = await response.json();

            if (data.success) {
                // Ouvrir la page de l'appel
                window.open(`/messages/${this.conversationIdValue}/call/${data.sessionId}/join`, '_blank', 'width=800,height=600');
            } else {
                alert(`Erreur lors de l'initiation de l'appel: ${data.detail}`);
            }
        } catch (err) {
            console.error('Erreur lors de l\'appel:', err);
            alert('Erreur lors de l\'initiation de l\'appel');
        }
    }

    // ============ MEETING SCHEDULING ============
    openScheduleMeeting(e) {
        e.preventDefault();
        const modal = this.element.querySelector('#scheduleMeetingModal');
        if (modal) {
            modal.classList.add('visible');
            modal.setAttribute('aria-hidden', 'false');
        }
    }

    closeScheduleMeeting(e) {
        e.preventDefault();
        const modal = this.element.querySelector('#scheduleMeetingModal');
        if (modal) {
            modal.classList.remove('visible');
            modal.setAttribute('aria-hidden', 'true');
        }
    }

    scheduleMeeting(e) {
        e.preventDefault();
        this.composerActionTarget.value = 'schedule_meeting';
        this.composerSendButtonTarget.click();
        this.closeScheduleMeeting(new Event('click'));
    }

    // ============ CHAT SUMMARIZATION ============
    async summarizeChat(e) {
        e.preventDefault();
        
        const button = e.target.closest('button');
        button.disabled = true;
        button.textContent = '✨ Résumé en cours...';

        try {
            const response = await fetch(`/api/conversations/${this.conversationIdValue}/summarize`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                },
            });

            const data = await response.json();

            if (data.success) {
                alert(`Résumé de la conversation:\n\n${data.summary}`);
            } else {
                alert(`Erreur: ${data.detail}`);
            }
        } catch (err) {
            console.error('Erreur lors du résumé:', err);
            alert('Erreur lors de la génération du résumé');
        } finally {
            button.disabled = false;
            button.textContent = '✨ Résumé IA';
        }
    }

    // ============ DYNAMIC SEARCH ============
    searchMessages(e) {
        const searchTerm = e.target.value.toLowerCase().trim();
        const messages = this.threadMessagesTarget.querySelectorAll('.message-bubble');

        messages.forEach(message => {
            const content = message.textContent.toLowerCase();
            const sender = message.querySelector('.message-sender');
            const senderText = sender ? sender.textContent.toLowerCase() : '';

            if (content.includes(searchTerm) || senderText.includes(searchTerm)) {
                message.style.display = '';
                message.classList.add('highlighted');
            } else {
                message.style.display = 'none';
                message.classList.remove('highlighted');
            }
        });
    }

    // ============ MESSAGE EDITING & DELETION ============
    editMessage(e) {
        e.preventDefault();
        const messageId = e.target.dataset.msgId;
        const messageBubble = e.target.closest('.message-bubble');
        const contentWrapper = messageBubble.querySelector('.message-bubble-content p');

        if (!contentWrapper) return;

        const currentText = contentWrapper.textContent;
        const newText = prompt('Éditer le message:', currentText);

        if (newText !== null && newText.trim() !== '') {
            this.submitEditMessage(messageId, newText.trim());
        }
    }

    async submitEditMessage(messageId, newContent) {
        try {
            const response = await fetch(`/api/messages/${messageId}/edit`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    content: newContent,
                }),
            });

            const data = await response.json();

            if (data.success) {
                location.reload();
            } else {
                alert(`Erreur: ${data.detail}`);
            }
        } catch (err) {
            console.error('Erreur lors de l\'édition:', err);
            alert('Erreur lors de l\'édition du message');
        }
    }

    deleteMessage(e) {
        e.preventDefault();
        const messageId = e.target.dataset.msgId;

        if (confirm('Voulez-vous vraiment supprimer ce message?')) {
            this.submitDeleteMessage(messageId);
        }
    }

    async submitDeleteMessage(messageId) {
        try {
            const response = await fetch(`/api/messages/${messageId}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
            });

            const data = await response.json();

            if (data.success) {
                location.reload();
            } else {
                alert(`Erreur: ${data.detail}`);
            }
        } catch (err) {
            console.error('Erreur lors de la suppression:', err);
            alert('Erreur lors de la suppression du message');
        }
    }

    // ============ SETUP EVENT LISTENERS ============
    setupEventListeners() {
        // Emoji picker
        if (this.hasEmojiToggleTarget) {
            this.emojiToggleTarget.addEventListener('click', (e) => this.toggleEmojiPicker(e));
        }

        const emojiButtons = this.element.querySelectorAll('.emoji-button');
        emojiButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.insertEmoji(e));
        });

        // File attachments
        if (this.hasAttachPhotoTarget) {
            this.attachPhotoTarget.addEventListener('click', (e) => this.attachPhoto(e));
        }
        if (this.hasAttachFileTarget) {
            this.attachFileTarget.addEventListener('click', (e) => this.attachFile(e));
        }

        const fileInputs = this.element.querySelectorAll('input[type="file"]');
        fileInputs.forEach(input => {
            input.addEventListener('change', (e) => this.handleFileSelect(e));
        });

        // Audio recording
        if (this.hasRecordAudioButtonTarget) {
            this.recordAudioButtonTarget.addEventListener('click', (e) => this.recordAudio(e));
        }

        // Calls
        if (this.hasStartAudioCallTarget) {
            this.startAudioCallTarget.addEventListener('click', (e) => this.startAudioCall(e));
        }
        if (this.hasStartVideoCallTarget) {
            this.startVideoCallTarget.addEventListener('click', (e) => this.startVideoCall(e));
        }

        // Chat summarization
        if (this.hasSummarizeChatTarget) {
            this.summarizeChatTarget.addEventListener('click', (e) => this.summarizeChat(e));
        }

        // Meeting scheduling
        if (this.hasOpenScheduleMeetingTarget) {
            this.openScheduleMeetingTarget.addEventListener('click', (e) => this.openScheduleMeeting(e));
        }

        const closeScheduleBtn = this.element.querySelector('#closeScheduleModal');
        if (closeScheduleBtn) {
            closeScheduleBtn.addEventListener('click', (e) => this.closeScheduleMeeting(e));
        }

        const cancelScheduleBtn = this.element.querySelector('#cancelSchedule');
        if (cancelScheduleBtn) {
            cancelScheduleBtn.addEventListener('click', (e) => this.closeScheduleMeeting(e));
        }

        const scheduleForm = this.element.querySelector('.schedule-form');
        if (scheduleForm) {
            scheduleForm.addEventListener('submit', (e) => this.scheduleMeeting(e));
        }

        // Dynamic search
        if (this.hasChatSearchInputTarget) {
            this.chatSearchInputTarget.addEventListener('input', (e) => this.searchMessages(e));
        }

        // Message editing & deletion
        const editButtons = this.element.querySelectorAll('.edit-message');
        editButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.editMessage(e));
        });

        const deleteButtons = this.element.querySelectorAll('.delete-message');
        deleteButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.deleteMessage(e));
        });

        // Schedule button
        if (this.hasScheduleButtonTarget) {
            this.scheduleButtonTarget.addEventListener('click', (e) => {
                e.preventDefault();
                this.composerActionTarget.value = 'schedule_message';
                // Show a schedule dialog or inline form
                const datetime = prompt('Planifier le message pour (format: YYYY-MM-DD HH:MM):');
                if (datetime) {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'scheduled_datetime';
                    input.value = datetime;
                    this.element.querySelector('.composer-form').appendChild(input);
                }
            });
        }
    }
}
