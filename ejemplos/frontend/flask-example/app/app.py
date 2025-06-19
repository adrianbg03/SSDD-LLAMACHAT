from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user, UserMixin
from flask_sqlalchemy import SQLAlchemy
from forms import LoginForm, RegisterForm

# Usuarios
from models import users, User

# Login
from forms import LoginForm

app = Flask(__name__, static_url_path='')
login_manager = LoginManager()
login_manager.init_app(app) # Para mantener la sesión

# Configurar el secret_key. OJO, no debe ir en un servidor git público.
# Python ofrece varias formas de almacenar esto de forma segura, que
# no cubriremos aquí.
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///users.db'
db = SQLAlchemy(app)

# Modelo de usuario
class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False)
    email = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(100), nullable=False)

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

# Ruta principal
@app.route('/')
def index():
    return render_template('index.html')

# @app.route('/login', methods=['GET', 'POST'])
#def login():
#   if current_user.is_authenticated:
#        return redirect(url_for('index'))
#    else:
#        error = None
#        form = LoginForm(None if request.method != 'POST' else request.form)
#        if request.method == "POST" and  form.validate():
#            if form.email.data != 'admin@um.es' or form.password.data != 'admin':
#                error = 'Invalid Credentials. Please try again.'
#            else:
#                user = User(1, 'admin', form.email.data.encode('utf-8'),
#                            form.password.data.encode('utf-8'))
#                users.append(user)
#                login_user(user, remember=form.remember_me.data)
#                return redirect(url_for('index'))

#        return render_template('login.html', form=form,  error=error)
    
# Ruta de login
@app.route('/login', methods=['GET', 'POST'])
def login():
    form = LoginForm()
    if form.validate_on_submit():
        user = User.query.filter_by(email=form.email.data).first()
        if user and user.password == form.password.data:
            login_user(user)
            flash('Login exitoso.', 'success')
            return redirect(url_for('index'))
        flash('Credenciales inválidas.', 'danger')
    return render_template('login.html', form=form)

# Ruta de registro
@app.route('/register', methods=['GET', 'POST'])
def register():
    form = RegisterForm()
    if form.validate_on_submit():
        if User.query.filter_by(email=form.email.data).first():
            flash('El email ya está registrado.', 'danger')
        else:
            new_user = User(username=form.username.data, email=form.email.data, password=form.password.data)
            db.session.add(new_user)
            db.session.commit()
            flash('Registro exitoso. Inicia sesión.', 'success')
            return redirect(url_for('login'))
    return render_template('register.html', form=form)

# Ruta del perfil de usuario
@app.route('/profile')
@login_required
def profile():
    return render_template('profile.html, user=current_user')

# Ruta de logout
@app.route('/logout')
@login_required
def logout():
    logout_user()
    flash('Sesión cerrada.', 'info')
    return redirect(url_for('index'))

@login_manager.user_loader
def load_user(user_id):
    for user in users:
        if user.id == int(user_id):
            return user
    return None

# Iniciar la base de datos
with app.app_context():
    db.create_all()

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
