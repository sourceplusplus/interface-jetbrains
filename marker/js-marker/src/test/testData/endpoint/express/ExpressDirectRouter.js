const express = require('express');
const app = express();

app.use('/test', require('./test-endpoint'));
