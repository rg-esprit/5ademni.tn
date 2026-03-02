"""
API Flask pour la prédiction de prix
"""

from flask import Flask, request, jsonify
import joblib
import numpy as np
import os

app = Flask(__name__)

# Chemins des modèles
MODEL_PATH = 'price_model.pkl'
SCALER_PATH = 'price_scaler.pkl'
# Use price-specific encoder first, fallback to shared encoder
ENCODER_PATH = 'price_category_encoder.pkl' if os.path.exists('price_category_encoder.pkl') else 'category_encoder.pkl'

# Variables globales pour les modèles
model = None
scaler = None
label_encoder = None

def load_models():
    """Charge tous les modèles nécessaires"""
    global model, scaler, label_encoder
    
    try:
        print("Chargement des modèles...")
        
        if not os.path.exists(MODEL_PATH):
            print(f"Erreur: {MODEL_PATH} introuvable")
            return False
        
        if not os.path.exists(SCALER_PATH):
            print(f"Erreur: {SCALER_PATH} introuvable")
            return False
        
        if not os.path.exists(ENCODER_PATH):
            print(f"Erreur: {ENCODER_PATH} introuvable")
            return False
        
        model = joblib.load(MODEL_PATH)
        print(f"✓ Modèle chargé: {MODEL_PATH}")
        
        scaler = joblib.load(SCALER_PATH)
        print(f"✓ Scaler chargé: {SCALER_PATH}")
        
        label_encoder = joblib.load(ENCODER_PATH)
        print(f"✓ Encodeur chargé: {ENCODER_PATH}")
        
        print(f"\nCatégories disponibles: {list(label_encoder.classes_)}")
        print("Tous les modèles chargés avec succès!")
        
        return True
    
    except Exception as e:
        print(f"Erreur lors du chargement des modèles: {str(e)}")
        return False

@app.route('/health', methods=['GET'])
def health():
    """Endpoint de santé"""
    if model is None or scaler is None or label_encoder is None:
        return jsonify({
            'status': 'error',
            'message': 'Modèles non chargés'
        }), 500
    
    return jsonify({
        'status': 'ok',
        'message': 'Service de prédiction de prix opérationnel',
        'available_categories': list(label_encoder.classes_)
    }), 200

@app.route('/predict_price', methods=['POST'])
def predict_price():
    """
    Prédire le prix d'une annonce
    
    Input JSON:
    {
        "category": "nom de la catégorie",
        "description_length": longueur de la description (int),
        "delivery_time": délai de livraison en jours (int)
    }
    
    Output JSON:
    {
        "recommended_price": prix prédit (float),
        "success": true
    }
    """
    try:
        # Vérifier que les modèles sont chargés
        if model is None or scaler is None or label_encoder is None:
            return jsonify({
                'success': False,
                'error': 'Modèles non chargés'
            }), 500
        
        # Récupérer les données de la requête
        data = request.get_json()
        
        if not data:
            return jsonify({
                'success': False,
                'error': 'Aucune donnée fournie'
            }), 400
        
        # Vérifier les paramètres requis
        required_params = ['category', 'description_length', 'delivery_time']
        missing_params = [p for p in required_params if p not in data]
        
        if missing_params:
            return jsonify({
                'success': False,
                'error': f'Paramètres manquants: {", ".join(missing_params)}',
                'required_params': required_params
            }), 400
        
        category = data['category']
        description_length = data['description_length']
        delivery_time = data['delivery_time']
        
        # Valider les types
        try:
            description_length = int(description_length)
            delivery_time = int(delivery_time)
        except ValueError:
            return jsonify({
                'success': False,
                'error': 'description_length et delivery_time doivent être des entiers'
            }), 400
        
        # Valider les valeurs
        if description_length < 0:
            return jsonify({
                'success': False,
                'error': 'description_length doit être positif'
            }), 400
        
        if delivery_time < 0:
            return jsonify({
                'success': False,
                'error': 'delivery_time doit être positif'
            }), 400
        
        # Vérifier que la catégorie existe
        if category not in label_encoder.classes_:
            return jsonify({
                'success': False,
                'error': f'Catégorie inconnue: {category}',
                'available_categories': list(label_encoder.classes_)
            }), 400
        
        # Encoder la catégorie
        category_encoded = label_encoder.transform([category])[0]
        
        # Créer le vecteur de features
        features = np.array([[category_encoded, description_length, delivery_time]])
        
        # Normaliser les features
        features_scaled = scaler.transform(features)
        
        # Faire la prédiction
        predicted_price = model.predict(features_scaled)[0]
        
        # S'assurer que le prix est positif
        predicted_price = max(0, predicted_price)
        
        return jsonify({
            'success': True,
            'recommended_price': round(float(predicted_price), 2),
            'input': {
                'category': category,
                'description_length': description_length,
                'delivery_time': delivery_time
            }
        }), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': f'Erreur lors de la prédiction: {str(e)}'
        }), 500

@app.route('/predict_price_batch', methods=['POST'])
def predict_price_batch():
    """
    Prédire les prix pour plusieurs annonces
    
    Input JSON:
    {
        "predictions": [
            {
                "category": "...",
                "description_length": ...,
                "delivery_time": ...
            },
            ...
        ]
    }
    
    Output JSON:
    {
        "predictions": [
            {
                "recommended_price": ...,
                "input": {...}
            },
            ...
        ],
        "success": true
    }
    """
    try:
        if model is None or scaler is None or label_encoder is None:
            return jsonify({
                'success': False,
                'error': 'Modèles non chargés'
            }), 500
        
        data = request.get_json()
        
        if not data or 'predictions' not in data:
            return jsonify({
                'success': False,
                'error': 'Paramètre "predictions" manquant'
            }), 400
        
        predictions_input = data['predictions']
        
        if not isinstance(predictions_input, list) or len(predictions_input) == 0:
            return jsonify({
                'success': False,
                'error': 'Le paramètre "predictions" doit être une liste non vide'
            }), 400
        
        results = []
        errors = []
        
        for i, item in enumerate(predictions_input):
            try:
                # Vérifier les paramètres
                if not all(k in item for k in ['category', 'description_length', 'delivery_time']):
                    errors.append({
                        'index': i,
                        'error': 'Paramètres manquants'
                    })
                    continue
                
                category = item['category']
                description_length = int(item['description_length'])
                delivery_time = int(item['delivery_time'])
                
                # Vérifier la catégorie
                if category not in label_encoder.classes_:
                    errors.append({
                        'index': i,
                        'error': f'Catégorie inconnue: {category}'
                    })
                    continue
                
                # Encoder et prédire
                category_encoded = label_encoder.transform([category])[0]
                features = np.array([[category_encoded, description_length, delivery_time]])
                features_scaled = scaler.transform(features)
                predicted_price = max(0, model.predict(features_scaled)[0])
                
                results.append({
                    'recommended_price': round(float(predicted_price), 2),
                    'input': {
                        'category': category,
                        'description_length': description_length,
                        'delivery_time': delivery_time
                    }
                })
            
            except Exception as e:
                errors.append({
                    'index': i,
                    'error': str(e)
                })
        
        response = {
            'success': True,
            'predictions': results,
            'count': len(results)
        }
        
        if errors:
            response['errors'] = errors
        
        return jsonify(response), 200
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': f'Erreur lors de la prédiction batch: {str(e)}'
        }), 500

@app.route('/categories', methods=['GET'])
def get_categories():
    """Retourne la liste des catégories disponibles"""
    if label_encoder is None:
        return jsonify({
            'success': False,
            'error': 'Encodeur non chargé'
        }), 500
    
    return jsonify({
        'success': True,
        'categories': list(label_encoder.classes_)
    }), 200

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
    # Charger les modèles au démarrage
    if load_models():
        print("\n========================================")
        print("Démarrage de l'API de prédiction de prix")
        print("========================================")
        print("\nEndpoints disponibles:")
        print("  GET  /health - Vérifier l'état du service")
        print("  GET  /categories - Lister les catégories disponibles")
        print("  POST /predict_price - Prédire un prix")
        print("  POST /predict_price_batch - Prédictions batch")
        print("\nExemple d'utilisation:")
        print('  curl -X POST http://localhost:5001/predict_price \\')
        print('    -H "Content-Type: application/json" \\')
        print('    -d \'{"category": "Développement Web", "description_length": 250, "delivery_time": 7}\'')
        print("\n========================================\n")
        
        app.run(host='0.0.0.0', port=5001, debug=False, use_reloader=False)
    else:
        print("Impossible de démarrer l'API: modèles non trouvés")
        print("Exécutez d'abord: python train_price.py")
