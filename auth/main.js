const { exec } = require("child_process");
const { promisify } = require("util");

const execPromise = promisify(exec);

async function getMAC(ip) {
    try {
        const { stdout } = await execPromise(`arp -n ${ip} | awk '/ether/ {print $3}'`);
        return stdout.trim();
    } catch (err) {
        console.error("Erro ao executar:", err);
        throw err;
    }
}

// Essa função pode não funcionar caso vocês tentem acessar alguma rota na mesma máquina que o servidor esteja rodando

async function verifyAdminMAC(ip) {
    // TODO: Buscar MAC real do admin no banco
    
    const macEsperado = "5e:51:83:b6:d1:75".toLowerCase();
    const macEncontrado = await getMAC(ip);

    console.log("Encontrado: ", macEncontrado)

    return macEncontrado === macEsperado ? true : false;
}

module.exports = {
    verifyAdminMAC
};
