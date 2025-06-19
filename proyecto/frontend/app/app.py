# Importación de módulos principales de Flask y librerías necesarias
from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
from forms import LoginForm, SignUpForm
from flask import jsonify,request, render_template
import requests
import json
import os

# Modelos de usuario y lista temporal de usuarios en memoria
from models import users, User

# Formulario de login
from forms import LoginForm
from datetime import datetime

# Inicializa la aplicación Flask y el sistema de login
app = Flask(__name__, static_url_path='') 
login_manager = LoginManager()
login_manager.init_app(app)

# Clave secreta
app.config['SECRET_KEY'] = '1234'

# Dirección del backend REST que recibe los prompts
BACKEND_URL = f'http://localhost:8080/prompt'

# Ruta para servir archivos estáticos (CSS, JS...)
@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

# Página de inicio (index)
@app.route('/')
def index():
    return render_template('index.html')

# Filtro para formatear fechas en las plantillas
@app.template_filter('datetimeformat')
def datetimeformat(value, format='%Y-%m-%d %H:%M:%S'):
    # Si no hay valor, devuelve vacío
    if not value:
        return ""
    # Si ya es objeto datetime, formatea directamente
    if isinstance(value, datetime):
        return value.strftime(format)
    # Si es string, intenta convertirlo
    if isinstance(value, str):
        for fmt in ("%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S"):
            try:
                dt = datetime.strptime(value[:19], fmt)
                return dt.strftime(format)
            except Exception:
                continue
        return value
    return str(value)

# Ruta para registro de usuarios nuevos
@app.route('/signup', methods=['GET', 'POST'])
def signup():
    form = SignUpForm(request.form if request.method == 'POST' else None)
    if request.method == "POST" and form.validate():
        # Envia datos al backend REST para registrar
        response = requests.post(
            'http://backend-rest:8080/Service/Registro',
            json={
                "name": form.name.data,
                "email": form.email.data,
                "password": form.password.data
            }
        )
        if response.status_code == 200:
            flash("¡Registro completado! Puedes iniciar sesión.", "success")
            return redirect(url_for('signup'))
        else:
            flash(f"Registro no completado. Código: {response.status_code}. Mensaje: {response.text}", "danger")
    return render_template('signup.html', form=form)

# Ruta para inicio de sesión (login)
@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    error = None
    form = LoginForm(None if request.method != 'POST' else request.form)
    if request.method == "POST" and form.validate():
        # Envia los datos al backend para comprobar login
        response = requests.post(
            'http://backend-rest:8080/Service/checkLogin',
            json={
                "email": form.email.data,
                "password": form.password.data
            }
        )
        if response.status_code == 200:
            user_data = response.json()
            user = User(user_data["id"], user_data["name"], form.email.data, form.password.data)
            login_user(user, remember=form.remember_me.data)
            users.append(user)
            return redirect(url_for('index'))
        else:
            error = 'Credenciales no válidas. Por favor, pruebe de nuevo.'
    return render_template('login.html', form=form, error=error)

# Ruta protegida: solo accesible si estás logueado
@app.route('/profile')
@login_required
def profile():
    response = requests.get(f'http://localhost:5010/api/user/{current_user.id}')
    if response.status_code == 200:
        response.json()
    else:
        flash('Error al obtener los datos del perfil.', 'danger')
    return render_template('profile.html')

# Cerrar sesión
@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

# Ruta para mostrar logs (conversaciones)
@app.route('/logs', methods=['GET', 'POST'])
@login_required
def logs():
    user_id = current_user.id
    url_base = f'http://backend-rest:8080/Service/u/{user_id}/dialogue'

    if request.method == 'POST':
        dialogue_id = request.form.get('dialogue_id')
        if dialogue_id:
            url_delete = f"{url_base}/{dialogue_id}"
            try:
                requests.delete(url_delete)
            except Exception as e:
                flash(f"Error al borrar: {e}", "danger")
        return redirect(url_for('logs'))

    # Recupera las conversaciones
    response = requests.get(url_base)
    conversaciones = response.json() if response.status_code == 200 else []
    # Intenta convertir los campos 'dialogue' a JSON
    for conv in conversaciones:
        try:
            conv['dialogue_obj'] = json.loads(conv['dialogue'])
        except Exception:
            conv['dialogue_obj'] = {}

    return render_template('logs.html', conversaciones=conversaciones)

# Ruta para ver estadísticas de uso
@app.route('/stats')
@login_required
def stats():
    user_id = current_user.id
    response = requests.get(f'http://backend-rest:8080/Service/stats', params={"userId": user_id})
    stats_data = response.json() if response.status_code == 200 else {}
    return render_template('stats.html', stats=stats_data)

# Función requerida por Flask-Login para cargar el usuario en sesión
@login_manager.user_loader
def load_user(user_id):
    for user in users:
        if str(user.id) == user_id:
            return user
    return None

# Ruta principal para enviar prompts (requiere estar logueado)
@app.route("/prompt", methods=["GET", "POST"])
@login_required
def prompt():
    if request.method == "POST":
        prompt = request.form.get("prompt")
        try:
            response = requests.post(BACKEND_URL, json={"prompt": prompt, "userId": current_user.id})
            if response.status_code == 200:
                try:
                    data = response.json()
                    if "response" in data:
                        return render_template("prompt.html", prompt=prompt, response=data["response"])
                    else:
                        return render_template("prompt.html", error="Respuesta inesperada del servidor", prompt=prompt)
                except ValueError:
                    return render_template("prompt.html", error="Error al procesar la respuesta del servidor", prompt=prompt)
            else:
                return render_template("prompt.html", error=f"Error en el servidor Java: {response.text}", prompt=prompt)
        except Exception as e:
            return render_template("prompt.html", error=str(e), prompt=prompt)

    return render_template("prompt.html")

# API auxiliar: proxy para reenviar el prompt al backend REST
@app.route("/Service/prompt", methods=["POST"])
def proxy_prompt_to_backend():
    try:
        data = request.get_json()
        if not data:
            return {"error": "No JSON received"}, 400
        # Si no se envió userId, se añade el actual
        if 'userId' not in data or data['userId'] is None:
            data['userId'] = current_user.id
        response = requests.post("http://backend-rest:8080/Service/prompt", json=data)
        return (response.text, response.status_code, response.headers.items())
    except Exception as e:
        return {"error": str(e)}, 500

# Ejecutar la aplicación si este archivo es el principal
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
