import os

import subprocess

from flask import Flask

app = Flask(__name__)


@app.route('/')
def hello():
    url = "http://" + str(os.environ['Primary']) + ":8000"
    print("URL:", url)
    r = int(subprocess.run(['curl', url], stdout=subprocess.PIPE).stdout)
    return '<h1>Hello, World!</h1>' + str(r)
