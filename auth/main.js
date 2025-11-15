const { exec } = require("child_process");

async function getMAC(ip){
    try {
        response = await exec(`arp -n ${ip} | awk '/ether/ {print $3}'`, (error, stdout, stderr) => {
            if (error) {
                console.error(`Erro ao executar: ${error.message}`);
                return;
            }
            if (stderr) {
                console.error(`stderr: ${stderr}`);
                return;
            }
            });

    } catch (error) {
        console.log(error)
    }
    return response
}

// Essa função não vai funcionar caso vocês tentem acessar alguma rota na mesma máquina que o "servidor" esteja rodando, isso acontece pq o ip da máquina na rede não fica mapeado, e sim o localhost

async function verifyAdminMAC(ip) {
    // TODO: Buscar MAC real do admin no macEsperado
    
    const macEsperado = "20:23:51:8b:3f:28".toLowerCase();
    const macEncontrado = await getMAC(ip);

    console.log("Encontrado: ", macEncontrado)

    if (!macEncontrado) return false;

    return macEncontrado === macEsperado;
}

module.exports = {
    verifyAdminMAC
};
