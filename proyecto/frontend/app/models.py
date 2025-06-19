from flask_login import UserMixin
import hashlib

users = []

class User(UserMixin):

    def __init__(self, id, name, email, password, is_admin=False):
        self.id = id
        self.name = name
        self.email = email
        self.password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        self.is_admin = is_admin

    def set_password(self, password):
        self.password = hashlib.sha256(password.encode('utf-8')).hexdigest()

    def check_password(self, password):
        return self.password == hashlib.sha256(password.encode('utf-8')).hexdigest()

    @staticmethod
    def get_user(email):
        for user in users:
            if user.email == email:
                return user
        return None
    
    def get_id(self):
        return str(self.id)

    def __repr__(self):
        return '<User {}>'.format(self.email)


#UserMixin es una clase de Flask_Login que proporciona metodos para manejar usuarios en la autenticación. 