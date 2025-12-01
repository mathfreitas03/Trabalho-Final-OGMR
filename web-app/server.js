const express = require("express");
const authMac = require("./auth/mac");
const path = require("path");
const db = require("./db")
const fetch = require("node-fetch"); // npm install node-fetch@2
const bcrypt = require("bcrypt")
const server = express();
const PORT = 5500
const JAVA_SERVER = "http://localhost:8080";

server.use(express.json());

server.get(`${"/index.html" || "/"}`, async (req, res) => {
    
    const ip = req.socket.remoteAddress.replace(/^::ffff:/, "");
    const accepted = await authMac.verifyAdminMAC(ip)

    if (accepted) {
        res.sendFile(path.join(__dirname, 'index.html'));
    } else {
        res.status(403).sendFile(path.join(__dirname, 'forbidden.html'));
    }
})

// server.get("/", async (req, res) => {
//     try {
//         const rows = await db.query("SELECT NOW()"); 

//         res.json(rows[0]);

//     } catch (err) {
//         console.error("Erro na query:", err);
//         res.status(500).json({ error: "Erro no banco" });
//     }
// });

server.post("/login.html", async (req, res) => {
  try {
    const { login, password } = req.body;

    const rows = await db.query(
        'SELECT id, login, "password" FROM admin_user WHERE login = $1',
        [login]
    );
    
    if (rows.length === 0) {
      return res.status(401).json({ error: "Credenciais inválidas" });
    }

    const user = rows[0];

    const passwordMatch = await bcrypt.compare(password, user.password);

    if (!passwordMatch) {
      return res.status(401).json({ error: "Credenciais inválidas" });
    }
        
    res.status(200).sendFile(path.join(__dirname, "main.html"));

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Erro interno do servidor" });
  }
});

server.get("/api/ports", async (req, res) => {
  try {
    // repassa a requisição GET para o Java
    const query = req.query.switch ? `?switch=${req.query.switch}` : "";
    const response = await fetch(`${JAVA_SERVER}/ports${query}`);
    const data = await response.json();
    res.json(data);
  } catch (err) {
    console.error("Erro ao obter portas do Java:", err);
    res.status(500).json({ error: "Falha ao buscar portas" });
  }
});

server.post("/api/port/block", async (req, res) => {
  try {
    const response = await fetch(`${JAVA_SERVER}/block`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body)
    });
    const data = await response.json();
    res.json(data);
  } catch (err) {
    console.error("Erro ao bloquear portas no Java:", err);
    res.status(500).json({ error: "Falha ao bloquear portas" });
  }
});

server.post("/api/port/unblock", async (req, res) => {
  try {
    const response = await fetch(`${JAVA_SERVER}/unblock`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body)
    });
    const data = await response.json();
    res.json(data);
  } catch (err) {
    console.error("Erro ao desbloquear portas no Java:", err);
    res.status(500).json({ error: "Falha ao desbloquear portas" });
  }
});

server.use(express.static('web-app'));
server.listen(PORT, ()=> {console.log("Rodando em " + PORT)})