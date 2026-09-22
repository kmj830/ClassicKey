document.addEventListener('DOMContentLoaded', () => {
    // Top Nav & Mode Switch
    const modeEncryptTab = document.getElementById('mode-encrypt');
    const modeDecryptTab = document.getElementById('mode-decrypt');
    const pillModeLabel = document.getElementById('pill-mode-label');

    // Inputs & Controls
    const textInput = document.getElementById('text-input');
    const textLabel = document.getElementById('text-label');
    const textCharCount = document.getElementById('text-char-count');
    const keySlider = document.getElementById('key-slider');
    const keyDisplayNum = document.getElementById('key-display-num');
    const btnOrbExecute = document.getElementById('btn-orb-execute');
    const btnExecute = document.getElementById('btn-execute');
    const btnReset = document.getElementById('btn-reset');
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');

    // Result Elements
    const resultSection = document.getElementById('result-section');
    const resultMetaLabel = document.getElementById('result-meta-label');
    const resultText = document.getElementById('result-text');
    const btnCopy = document.getElementById('btn-copy');
    const btnSendOpposite = document.getElementById('btn-send-opposite');
    const stepsTableBody = document.getElementById('steps-tbody');

    // Mapping Table Elements
    const tableToggle = document.getElementById('table-toggle');
    const accordionArrow = document.getElementById('accordion-arrow');
    const tableContent = document.getElementById('table-content');
    const mappingGrid = document.getElementById('mapping-grid');
    const modulusBadge = document.getElementById('modulus-badge');
    const toast = document.getElementById('toast');

    // State
    let currentMode = 'ENCRYPT';
    let modulus = 62;
    let tableEntries = [];

    // Initialize Table Data
    fetchTableInfo();

    // Mode Switching
    modeEncryptTab.addEventListener('click', () => setMode('ENCRYPT'));
    modeDecryptTab.addEventListener('click', () => setMode('DECRYPT'));

    function setMode(mode) {
        currentMode = mode;
        if (mode === 'ENCRYPT') {
            modeEncryptTab.classList.add('active');
            modeDecryptTab.classList.remove('active');
            pillModeLabel.innerHTML = '<span>🔒 암호화 (ENCRYPT)</span>';
            textLabel.textContent = '평문 텍스트 (Plaintext)';
            textInput.placeholder = '암호화할 평문 텍스트를 입력하세요 (영대소문자, 숫자)... e.g. ClassicKey';
            btnExecute.innerHTML = '<span>🔒</span> 암호화 실행 (Run)';
            btnSendOpposite.innerHTML = '🔓 복호화 모드로 전환';
            resultMetaLabel.textContent = '변환 완료 • 암호문 (Ciphertext)';
        } else {
            modeDecryptTab.classList.add('active');
            modeEncryptTab.classList.remove('active');
            pillModeLabel.innerHTML = '<span>🔓 복호화 (DECRYPT)</span>';
            textLabel.textContent = '암호문 텍스트 (Ciphertext)';
            textInput.placeholder = '복호화할 한글 자모 암호문을 입력하세요... e.g. ㄺㅖㅋㅝㅝㅓㅍㅸㅏㅣ';
            btnExecute.innerHTML = '<span>🔓</span> 복호화 실행 (Run)';
            btnSendOpposite.innerHTML = '🔒 암호화 모드로 전환';
            resultMetaLabel.textContent = '복원 완료 • 평문 (Plaintext)';
        }
        hideAlert();
    }

    // Key Slider Synchronization
    keySlider.addEventListener('input', (e) => {
        keyDisplayNum.textContent = e.target.value;
    });

    // Character Counter
    textInput.addEventListener('input', () => {
        textCharCount.textContent = `${textInput.value.length}자`;
        hideAlert();
    });

    // Shortcut: Ctrl+Enter / Cmd+Enter to execute
    textInput.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            executeCipher();
        }
    });

    // Execute Buttons
    btnExecute.addEventListener('click', executeCipher);
    btnOrbExecute.addEventListener('click', executeCipher);

    // Reset Button
    btnReset.addEventListener('click', () => {
        textInput.value = '';
        textCharCount.textContent = '0자';
        hideAlert();
        resultSection.classList.remove('show');
    });

    // Presets Bar
    document.querySelectorAll('.preset-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const val = chip.getAttribute('data-val');
            const keyVal = chip.getAttribute('data-key');
            const modeVal = chip.getAttribute('data-mode');

            if (modeVal) setMode(modeVal);
            if (keyVal) {
                keySlider.value = keyVal;
                keyDisplayNum.textContent = keyVal;
            }
            textInput.value = val;
            textCharCount.textContent = `${val.length}자`;
            executeCipher();
        });
    });

    // Copy Button
    btnCopy.addEventListener('click', () => {
        const text = resultText.textContent;
        if (!text || text === '-') return;
        navigator.clipboard.writeText(text).then(() => {
            showToast('클립보드에 복사되었습니다.');
        }).catch(() => {
            const temp = document.createElement('textarea');
            temp.value = text;
            document.body.appendChild(temp);
            temp.select();
            document.execCommand('copy');
            document.body.removeChild(temp);
            showToast('클립보드에 복사되었습니다.');
        });
    });

    // Send to opposite mode
    btnSendOpposite.addEventListener('click', () => {
        const text = resultText.textContent;
        if (!text || text === '-') return;
        const targetMode = (currentMode === 'ENCRYPT') ? 'DECRYPT' : 'ENCRYPT';
        setMode(targetMode);
        textInput.value = text;
        textCharCount.textContent = `${text.length}자`;
        executeCipher();
    });

    // Table Accordion Toggle
    tableToggle.addEventListener('click', () => {
        const isOpen = tableContent.classList.toggle('show');
        accordionArrow.textContent = isOpen ? '▲' : '▼';
    });

    // API Call Execution
    async function executeCipher() {
        const text = textInput.value;
        const key = parseInt(keySlider.value, 10);

        if (!text || text.trim() === '') {
            showAlert('변환할 텍스트를 입력해주세요.');
            textInput.focus();
            return;
        }

        if (isNaN(key) || key < 1 || key > modulus - 1) {
            showAlert(`대칭키 K는 1 이상 ${modulus - 1} 이하의 정수여야 합니다.`);
            return;
        }

        hideAlert();
        btnExecute.disabled = true;
        btnOrbExecute.disabled = true;

        try {
            const response = await fetch('/api/cipher', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    text: text,
                    key: key,
                    mode: currentMode
                })
            });

            const data = await response.json();

            if (!response.ok) {
                showAlert(data.message || '요청 처리 중 오류가 발생했습니다.');
                resultSection.classList.remove('show');
            } else {
                renderResult(data);
            }
        } catch (err) {
            showAlert('서버와의 통신에 실패했습니다: ' + err.message);
        } finally {
            btnExecute.disabled = false;
            btnOrbExecute.disabled = false;
        }
    }

    // Render Steps & Results
    function renderResult(data) {
        resultText.textContent = data.resultText;

        stepsTableBody.innerHTML = '';
        data.steps.forEach((step, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="color: var(--colors-muted); font-family: var(--font-mono); font-size: 13px;">${idx + 1}</td>
                <td><span class="cell-char-badge">${escapeHtml(step.char)}</span></td>
                <td><span class="cell-code">${step.code}</span></td>
                <td><span class="cell-formula">${escapeHtml(step.formula)}</span></td>
                <td><span class="cell-code">${step.resultCode}</span></td>
                <td><span class="cell-char-badge cell-result-char">${escapeHtml(step.resultChar)}</span></td>
            `;
            stepsTableBody.appendChild(tr);
        });

        resultSection.classList.add('show');
        resultSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // Fetch Table Information
    async function fetchTableInfo() {
        try {
            const res = await fetch('/api/cipher/table');
            if (res.ok) {
                const data = await res.json();
                modulus = data.modulus || 62;
                tableEntries = data.entries || [];
                renderTableGrid(tableEntries);

                keySlider.max = modulus - 1;
                modulusBadge.textContent = `Modulus n = ${modulus}`;
            }
        } catch (e) {
            console.error('Failed to fetch table info:', e);
        }
    }

    // Render Character Grid
    function renderTableGrid(entries) {
        mappingGrid.innerHTML = '';
        entries.forEach(entry => {
            const tile = document.createElement('div');
            tile.className = 'mapping-tile';
            tile.title = `클릭하여 추가`;
            tile.innerHTML = `
                <span class="tile-code">#${entry.code}</span>
                <span class="tile-pair">
                    <span class="tile-plain">${escapeHtml(entry.plainChar)}</span>
                    <span class="tile-arrow">→</span>
                    <span class="tile-cipher">${escapeHtml(entry.cipherChar)}</span>
                </span>
            `;
            tile.addEventListener('click', () => {
                const targetChar = (currentMode === 'ENCRYPT') ? entry.plainChar : entry.cipherChar;
                textInput.value += targetChar;
                textCharCount.textContent = `${textInput.value.length}자`;
                showToast(`'${targetChar}' 문자가 입력창에 추가되었습니다.`);
            });
            mappingGrid.appendChild(tile);
        });
    }

    function showAlert(msg) {
        alertMessage.textContent = msg;
        alertBox.style.display = 'flex';
    }

    function hideAlert() {
        alertBox.style.display = 'none';
    }

    function showToast(msg) {
        toast.textContent = msg;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 2200);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
});
