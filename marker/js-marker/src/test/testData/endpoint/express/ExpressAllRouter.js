const express = require('express');
const app = express();

const testEndpoint = require('./all-endpoint');
app.use('/test', testEndpoint);
app.use('/test2', testEndpoint);
