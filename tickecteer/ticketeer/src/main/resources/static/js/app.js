/**
 * Ticketeer - Application de controle des billets
 */

const MOTIF_LABELS = {
    'OK': 'Billet valide avec succes',
    'BILLET_INCONNU': 'Ce billet n\'est pas reconnu dans le systeme',
    'DEJA_VALIDE': 'Ce billet a deja ete valide pour ce service',
    'NON_CONFORME_SERVICE': 'Ce billet ne correspond pas au service selectionne',
    'HORS_PARCOURS_AUTORISE': 'Le voyageur est controle au-dela de la destination autorisee',
    'AVANT_ZONE_DE_VALIDITE': 'Le voyageur est controle avant sa zone de validite',
    'TRAJET_TERMINE': 'Ce trajet a deja ete termine pour ce billet',
    'VALIDATION_IMPOSSIBLE_TEMPORAIREMENT': 'La validation est temporairement indisponible',
    'CODE_ILLISIBLE': 'Le code optique est illisible ou invalide',
    'QR_SIGNATURE_INVALIDE': 'La signature de securite du QR code est invalide'
};

function validerBillet() {
    var codeOptique = document.getElementById('codeOptique').value.trim();
    var serviceId = document.getElementById('serviceId').value;
    var checkpointId = document.getElementById('checkpointId').value;
    var btn = document.getElementById('btnValidate');

    if (!codeOptique) {
        document.getElementById('codeOptique').focus();
        document.getElementById('codeOptique').style.borderColor = '#ef4444';
        setTimeout(function() {
            document.getElementById('codeOptique').style.borderColor = '';
        }, 2000);
        return;
    }

    if (!checkpointId) {
        document.getElementById('checkpointId').focus();
        document.getElementById('checkpointId').style.borderColor = '#ef4444';
        setTimeout(function() {
            document.getElementById('checkpointId').style.borderColor = '';
        }, 2000);
        return;
    }

    btn.classList.add('loading');
    btn.disabled = true;

    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/controleur/api/valider', true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
            btn.classList.remove('loading');
            btn.disabled = false;

            if (xhr.status === 200) {
                var data = JSON.parse(xhr.responseText);
                afficherResultat(data);
            } else {
                afficherErreur('Erreur de communication avec le serveur (code ' + xhr.status + ')');
            }
        }
    };

    xhr.onerror = function() {
        btn.classList.remove('loading');
        btn.disabled = false;
        afficherErreur('Impossible de contacter le serveur');
    };

    xhr.send(JSON.stringify({
        codeOptique: codeOptique,
        serviceId: serviceId,
        checkpointId: checkpointId
    }));
}

function afficherResultat(data) {
    var overlay = document.getElementById('resultOverlay');
    var icon = document.getElementById('resultIcon');
    var iconValid = document.getElementById('iconValid');
    var iconInvalid = document.getElementById('iconInvalid');
    var status = document.getElementById('resultStatus');
    var motif = document.getElementById('resultMotif');
    var clientSection = document.getElementById('clientSection');
    var serviceSection = document.getElementById('serviceSection');

    var isValid = data.resultat === 'VALID';

    icon.className = 'result-icon ' + (isValid ? 'valid' : 'invalid');
    iconValid.style.display = isValid ? 'block' : 'none';
    iconInvalid.style.display = isValid ? 'none' : 'block';

    status.textContent = isValid ? 'BILLET VALIDE' : 'BILLET INVALIDE';
    status.className = 'result-status ' + (isValid ? 'valid' : 'invalid');

    motif.textContent = MOTIF_LABELS[data.motif] || data.motif;

    if (data.clientNom || data.clientPrenom) {
        clientSection.style.display = 'block';
        var nom = (data.clientPrenom || '') + ' ' + (data.clientNom || '');
        document.getElementById('clientName').textContent = nom.trim();
        var initials = '';
        if (data.clientPrenom) initials += data.clientPrenom.charAt(0);
        if (data.clientNom) initials += data.clientNom.charAt(0);
        document.getElementById('clientAvatar').textContent = initials.toUpperCase();
    } else {
        clientSection.style.display = 'none';
    }

    if (data.serviceTrain || data.serviceTrajet || data.serviceDate) {
        serviceSection.style.display = 'block';
        document.getElementById('resTrain').textContent = data.serviceTrain || '-';
        document.getElementById('resTrajet').textContent = data.serviceTrajet || '-';
        document.getElementById('resDate').textContent = data.serviceDate || '-';
        document.getElementById('resCheckpoint').textContent = data.checkpointControle || '-';
        document.getElementById('resValidity').textContent = data.zoneValidite || '-';
    } else {
        serviceSection.style.display = 'none';
    }

    overlay.classList.add('active');
}

function afficherErreur(message) {
    var overlay = document.getElementById('resultOverlay');
    var icon = document.getElementById('resultIcon');
    var iconValid = document.getElementById('iconValid');
    var iconInvalid = document.getElementById('iconInvalid');
    var status = document.getElementById('resultStatus');
    var motif = document.getElementById('resultMotif');

    icon.className = 'result-icon invalid';
    iconValid.style.display = 'none';
    iconInvalid.style.display = 'block';
    status.textContent = 'ERREUR';
    status.className = 'result-status invalid';
    motif.textContent = message;

    document.getElementById('clientSection').style.display = 'none';
    document.getElementById('serviceSection').style.display = 'none';

    overlay.classList.add('active');
}

function nouveauScan() {
    var overlay = document.getElementById('resultOverlay');
    overlay.classList.remove('active');
    document.getElementById('codeOptique').value = '';
    document.getElementById('codeOptique').focus();
}

// Enter key triggers validation
document.addEventListener('DOMContentLoaded', function() {
    var input = document.getElementById('codeOptique');
    if (input) {
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                validerBillet();
            }
        });
    }
});
