const express = require("express");
const auth = require("./auth/main");
const path = require("path");

const server = express();
const PORT = 5500

server.get('/', async (req, res) => {
    
    const ip = req.socket.remoteAddress.replace(/^::ffff:/, "");
    const accepted = await auth.verifyAdminMAC(ip)

    if (accepted) {
        res.sendFile(path.join(__dirname, 'web-app', 'index.html'));
    } else {
        res.status(403).sendFile(path.join(__dirname, 'web-app', 'forbidden.html'));
    }
})

server.use(express.static('web-app'));
server.listen(PORT, ()=> {console.log("Rodando em " + PORT)})