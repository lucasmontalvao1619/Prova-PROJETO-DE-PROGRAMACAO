const formulario = document.getElementById('formulario');
const campoId = document.getElementById('id');
const campoDescricao = document.getElementById('descricao');
const campoValor = document.getElementById('valor');
const campoCategoria = document.getElementById('categoria');
const botaoCancelar = document.getElementById('cancelar');
const titulo = document.getElementById('tituloFormulario');
const erro = document.getElementById('erro');

const escapar = (texto) => String(texto)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

const emReais = (n) => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

async function api(caminho, opcoes = {}) {
    const resposta = await fetch('/api' + caminho, {
        headers: { 'Content-Type': 'application/json' }, ...opcoes
    });
    if (!resposta.ok) {
        const corpo = await resposta.json().catch(() => ({}));
        throw new Error(corpo.detail || 'Não foi possível completar a operação.');
    }
    return resposta.status === 204 ? null : resposta.json();
}

const mostrarErro = (m) => { erro.textContent = m; erro.hidden = false; };

async function carregarCategorias() {
    const categorias = await api('/categorias');
    campoCategoria.innerHTML = categorias
        .map(c => `<option value="${escapar(c.nome)}">${escapar(c.rotulo)} (${c.tipo.toLowerCase()})</option>`)
        .join('');
}

async function carregarTransacoes() {
    const transacoes = await api('/transacoes');
    document.getElementById('listaVazia').hidden = transacoes.length > 0;

    document.getElementById('listaTransacoes').innerHTML = transacoes.map(t => {
        const despesa = t.tipo === 'DESPESA';
        return `<tr>
            <td>${escapar(t.descricao)}</td>
            <td>${escapar(t.categoriaRotulo)}</td>
            <td class="direita ${despesa ? 'despesa' : 'receita'}">
                ${despesa ? '-' : '+'} ${emReais(t.valor)}
            </td>
            <td><div class="acoes">
                <button type="button" data-editar="${t.id}">Editar</button>
                <button type="button" data-apagar="${t.id}">Apagar</button>
            </div></td>
        </tr>`;
    }).join('');
}

async function carregarResumo() {
    const r = await api('/resumo');
    document.getElementById('saldo').textContent = emReais(r.saldo);
    document.getElementById('receitas').textContent = emReais(r.totalReceitas);
    document.getElementById('despesas').textContent = emReais(r.totalDespesas);

    document.getElementById('gastosVazio').hidden = r.gastosPorCategoria.length > 0;
    document.getElementById('gastos').innerHTML = r.gastosPorCategoria
        .map(g => `<li><span>${escapar(g.rotulo)}</span><strong>${emReais(g.total)}</strong></li>`)
        .join('');
}

const recarregar = () => Promise.all([carregarTransacoes(), carregarResumo()]);

function limpar() {
    formulario.reset();
    campoId.value = '';
    titulo.textContent = 'Novo lançamento';
    botaoCancelar.hidden = true;
    erro.hidden = true;
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    const editando = campoId.value !== '';

    try {
        await api(editando ? `/transacoes/${campoId.value}` : '/transacoes', {
            method: editando ? 'PUT' : 'POST',
            body: JSON.stringify({
                descricao: campoDescricao.value,
                valor: campoValor.value,
                categoria: campoCategoria.value
            })
        });
        limpar();
        await recarregar();
    } catch (e) {
        mostrarErro(e.message);
    }
});

botaoCancelar.addEventListener('click', limpar);

document.getElementById('listaTransacoes').addEventListener('click', async (evento) => {
    const botao = evento.target.closest('button');
    if (!botao) return;

    try {
        if (botao.dataset.editar) {
            const t = await api(`/transacoes/${botao.dataset.editar}`);
            campoId.value = t.id;
            campoDescricao.value = t.descricao;
            campoValor.value = t.valor;
            campoCategoria.value = t.categoria;
            titulo.textContent = `Editando: ${t.descricao}`;
            botaoCancelar.hidden = false;
            campoDescricao.focus();
        } else if (botao.dataset.apagar) {
            await api(`/transacoes/${botao.dataset.apagar}`, { method: 'DELETE' });
            limpar();
            await recarregar();
        }
    } catch (e) {
        mostrarErro(e.message);
    }
});

carregarCategorias().then(recarregar).catch(e => mostrarErro(e.message));
