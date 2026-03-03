"""
API Flask pour la prédiction de catégories
"""

from flask import Flask, request, jsonify
import joblib
import os

app = Flask(__name__)

# Charger le modèle au démarrage
MODEL_PATH = 'category_classifier.pkl'
model = None

def load_model():
    """Charge le modèle de classification"""
    global model
    if os.path.exists(MODEL_PATH):
        print(f"Chargement du modèle depuis {MODEL_PATH}...")
        model = joblib.load(MODEL_PATH)
        print("Modèle chargé avec succès!")
        return True
    else:
        print(f"Erreur: Modèle introuvable à {MODEL_PATH}")
        print("Veuillez d'abord entraîner le modèle avec train_classifier.py")
        return False

@app.route('/health', methods=['GET'])
def health():
    """Endpoint de santé"""
    if model is None:
        return jsonify({
            'status': 'error',
            'message': 'Modèle non chargé'
        }), 500
    
    return jsonify({
        'status': 'ok',
        'message': 'Service de classification opérationnel'
    }), 200

@app.route('/predict_category', methods=['POST'])
def predict_category():
    """
    Prédire la catégorie d'une annonce
    
    Input JSON:
    {
        "text": "titre et description combinés"
    }
    
    Output JSON:
    {
        "category": "catégorie prédite",
        "success": true
    }
    """
    try:
        # Vérifier que le modèle est chargé
        if model is None:
            return jsonify({
                'success': False,
                'error': 'Modèle non chargé'
            }), 500
        
        # Récupérer les données de la requête
        data = request.get_json()
        
        if not data or 'text' not in data:
            return jsonify({
                'success': False,
                'error': 'Paramètre "text" manquant dans la requête'
            }), 400
        
        text = data['text']
        
        # Vérifier que le texte n'est pas vide
        if not text or text.strip() == '':
            return jsonify({
                'success': False,
                'error': 'Le texte ne peut pas être vide'
            }), 400
        
        # Faire la prédiction
        prediction = model.predict([text])[0]
        
        # Obtenir les probabilités (si disponible)
        try:
            probabilities = model.predict_proba([text])[0]
            classes = model.classes_
            
            # Créer un dictionnaire de probabilités
            prob_dict = {
                str(cls): float(prob) 
                for cls, prob in zip(classes, probabilities)
            }
            
            return jsonify({
                'success': True,
                'category': str(prediction),
                'probabilities': prob_dict,
                'confidence': float(max(probabilities))
            }), 200
            
        except AttributeError:
            # Si predict_proba n'est pas disponible
            return jsonify({
                'success': True,
                'category': str(prediction)
            }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': f'Erreur lors de la prédiction: {str(e)}'
        }), 500

@app.route('/predict_category_batch', methods=['POST'])
def predict_category_batch():
    """
    Prédire les catégories pour plusieurs textes
    
    Input JSON:
    {
        "texts": ["texte1", "texte2", ...]
    }
    
    Output JSON:
    {
        "predictions": [
            {"text": "texte1", "category": "cat1"},
            {"text": "texte2", "category": "cat2"}
        ],
        "success": true
    }
    """
    try:
        if model is None:
            return jsonify({
                'success': False,
                'error': 'Modèle non chargé'
            }), 500
        
        data = request.get_json()
        
        if not data or 'texts' not in data:
            return jsonify({
                'success': False,
                'error': 'Paramètre "texts" manquant dans la requête'
            }), 400
        
        texts = data['texts']
        
        if not isinstance(texts, list) or len(texts) == 0:
            return jsonify({
                'success': False,
                'error': 'Le paramètre "texts" doit être une liste non vide'
            }), 400
        
        # Faire les prédictions
        predictions = model.predict(texts)
        
        # Formater le résultat
        results = [
            {
                'text': text[:100] + '...' if len(text) > 100 else text,
                'category': str(pred)
            }
            for text, pred in zip(texts, predictions)
        ]
        
        return jsonify({
            'success': True,
            'predictions': results,
            'count': len(results)
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': f'Erreur lors de la prédiction batch: {str(e)}'
        }), 500

@app.errorhandler(404)
def not_found(error):
    """Gestionnaire d'erreur 404"""
    return jsonify({
        'success': False,
        'error': 'Endpoint introuvable'
    }), 404

@app.errorhandler(500)
def internal_error(error):
    """Gestionnaire d'erreur 500"""
    return jsonify({
        'success': False,
        'error': 'Erreur interne du serveur'
    }), 500

if __name__ == '__main__':
    # Charger le modèle au démarrage
    if load_model():
        print("\n========================================")
        print("Démarrage de l'API de classification")
        print("========================================")
        print("\nEndpoints disponibles:")
        print("  GET  /health - Vérifier l'état du service")
        print("  POST /predict_category - Prédire une catégorie")
        print("  POST /predict_category_batch - Prédictions batch")
        print("\nExemple d'utilisation:")
        print('  curl -X POST http://localhost:5000/predict_category \\')
        print('    -H "Content-Type: application/json" \\')
        print('    -d \'{"text": "Développeur Python Junior"}\'')
        print("\n========================================\n")
        
        app.run(host='0.0.0.0', port=5000, debug=False, use_reloader=False)
    else:
        print("Impossible de démarrer l'API: modèle non trouvé")
        print("Exécutez d'abord: python train_classifier.py")
