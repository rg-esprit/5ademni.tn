import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static values = {
        sessionId: String,
        conversationId: Number,
        callType: String,
        userId: Number,
    }

    connect() {
        this.localStream = null;
        this.peerConnection = null;
        this.remoteStream = null;
        this.setupCall();
    }

    async setupCall() {
        try {
            // Obtenir le flux audio/vidéo local
            const constraints = {
                audio: true,
                video: this.callTypeValue === 'video',
            };

            this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
            
            // Afficher le flux local
            const localVideo = document.querySelector('#localVideo');
            if (localVideo) {
                localVideo.srcObject = this.localStream;
            }

            // Créer la connexion P2P
            await this.createPeerConnection();
        } catch (err) {
            console.error('Erreur lors de la configuration de l\'appel:', err);
            alert('Erreur d\'accès au microphone/caméra');
        }
    }

    async createPeerConnection() {
        const config = {
            iceServers: [
                { urls: ['stun:stun.l.google.com:19302'] },
                { urls: ['stun:stun1.l.google.com:19302'] },
            ],
        };

        this.peerConnection = new RTCPeerConnection(config);

        // Ajouter le flux local aux pistes
        this.localStream.getTracks().forEach(track => {
            this.peerConnection.addTrack(track, this.localStream);
        });

        // Écouter les pistes distantes
        this.peerConnection.ontrack = (event) => {
            console.log('Piste distante reçue:', event.track);
            this.remoteStream = event.streams[0];
            const remoteVideo = document.querySelector('#remoteVideo');
            if (remoteVideo) {
                remoteVideo.srcObject = this.remoteStream;
            }
        };

        // Gérer les changements de connexion
        this.peerConnection.onconnectionstatechange = () => {
            console.log('État de la connexion:', this.peerConnection.connectionState);
            
            if (this.peerConnection.connectionState === 'failed' ||
                this.peerConnection.connectionState === 'disconnected') {
                this.endCall();
            }
        };

        // Setup ICE candidates
        this.peerConnection.onicecandidate = (event) => {
            if (event.candidate) {
                console.log('Envoi du candidat ICE:', event.candidate);
                this.sendCandidate(event.candidate);
            }
        };
    }

    async sendCandidate(candidate) {
        try {
            await fetch(`/api/call/${this.sessionIdValue}/candidate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    candidate: candidate.candidate,
                    sdpMLineIndex: candidate.sdpMLineIndex,
                    sdpMid: candidate.sdpMid,
                }),
            });
        } catch (err) {
            console.error('Erreur lors de l\'envoi du candidat ICE:', err);
        }
    }

    async makeOffer() {
        try {
            const offer = await this.peerConnection.createOffer();
            await this.peerConnection.setLocalDescription(offer);
            
            await fetch(`/api/call/${this.sessionIdValue}/offer`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    sdp: offer.sdp,
                    type: offer.type,
                }),
            });
        } catch (err) {
            console.error('Erreur lors de la création de l\'offre:', err);
        }
    }

    async handleRemoteOffer(offer) {
        try {
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
            const answer = await this.peerConnection.createAnswer();
            await this.peerConnection.setLocalDescription(answer);

            await fetch(`/api/call/${this.sessionIdValue}/answer`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    sdp: answer.sdp,
                    type: answer.type,
                }),
            });
        } catch (err) {
            console.error('Erreur lors de la gestion de l\'offre distante:', err);
        }
    }

    async handleRemoteAnswer(answer) {
        try {
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
        } catch (err) {
            console.error('Erreur lors de la gestion de la réponse distante:', err);
        }
    }

    async handleRemoteCandidate(candidate) {
        try {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
        } catch (err) {
            console.error('Erreur lors de l\'ajout du candidat ICE:', err);
        }
    }

    endCall() {
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
        }

        if (this.peerConnection) {
            this.peerConnection.close();
        }

        // Rediriger vers la conversation
        window.location.href = `/messages/${this.conversationIdValue}`;
    }

    disconnect() {
        this.endCall();
    }
}
