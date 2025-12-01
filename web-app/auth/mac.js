const { exec } = require("child_process");
const { promisify } = require("util");
const db = require("../db")

const execPromise = promisify(exec);

async function getMAC(ip) {
    let os = process.platform;

    switch(os){
        case "win32":
    try {
        await execPromise(`ping -n 1 ${ip}`);

        const { stdout } = await execPromise(`arp -a ${ip}`);
        const match = stdout.match(/([0-9a-f]{2}-){5}[0-9a-f]{2}/i);
        if (match != null){
            let formatedArp = match[0].replace("-",":")
            return formatedArp
        }
        else return null

    } catch (err) {
        console.error("Erro:", err);
        return null;
    }
        break;
        case "linux":
            try {
                const { stdout } = await execPromise(`arp -n ${ip} | awk '/ether/ {print $3}'`);
                return stdout.trim();
            } catch (err) {
                console.error("Erro ao executar:", err);
                throw err;
            }
        break;
    }
}

// Essa função pode não funcionar caso vocês tentem acessar alguma rota na mesma máquina que o servidor esteja rodando

async function verifyAdminMAC(ip) {
    // TODO: Buscar MAC real do admin no banco
    const row = await db.query("select mac from admin_user where id=1;")
    const macBanco = row[0].mac;
    console.log(macBanco)
    
    // const macEsperado = "c2:22-06-f8-c1-6d"
    const macEsperado = null
    // const macEncontrado = await getMAC(ip);
    const macEncontrado = '00:AA:06:00:00:01'

    console.log("Mac Banco:", macBanco)
    console.log("Mac Encontrado:", macEncontrado)
    return macEncontrado === macBanco ? true : false;
}

module.exports = {
    verifyAdminMAC
};
