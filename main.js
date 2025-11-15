const express = require("express");
const auth = require("./auth/main")


const server = express();
const PORT = 5500

server.get('/', (req, res) => {
    
    if(auth.verifyAdminMAC(req.connection.remoteAddress.replace(/^::ffff:/, ""))) {
        res.send("teste")
    }
    else{
        res.status(403).send("Esta máquina não está autorizada a acessar este serviço.")
    }

})

server.listen(PORT, ()=> {console.log("Rodando em " + PORT)})