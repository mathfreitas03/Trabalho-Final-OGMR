const express = require("express");
const auth = require("./auth/main");
const path = require("path");

const server = express();
const PORT = 5500

server.get('/', (req, res) => {
    
    if(auth.verifyAdminMAC(req.connection.remoteAddress.replace(/^::ffff:/, ""))) {
        res.sendFile(path.join(__dirname, 'web-app', 'index.html'))
    }
    else{
        res.status(403).send("Esta máquina não está autorizada a acessar este serviço.")
    }

})

server.use(express.static('web-app'));
server.listen(PORT, ()=> {console.log("Rodando em " + PORT)})