const express = require('express');
const router = express.Router();

router.get('/hello-world', (req, res) => {
    res.sendStatus(200);
});

module.exports = router;
