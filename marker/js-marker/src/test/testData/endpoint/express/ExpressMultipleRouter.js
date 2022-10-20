const express = require('express');
const app = express();

const testEndpoint = require('./test-endpoint');
app.use('/test', testEndpoint);
app.use('/test2', testEndpoint);
