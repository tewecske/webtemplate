const inputIds = ['note-a', 'note-b', 'note-c']

const backendSelect = document.getElementById('backend-select')

function apiBase() {
  return `/api/${backendSelect.value}`
}

function container(inputId) {
  return document.querySelector(`[data-input-id="${inputId}"]`)
}

async function save(inputId, value) {
  await fetch(`${apiBase()}/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ inputId, value }),
  })
}

async function refreshHistory(inputId) {
  const res = await fetch(`${apiBase()}/entries/${inputId}`)
  const entries = await res.json()
  render(inputId, entries)
}

function render(inputId, entries) {
  const list = container(inputId).querySelector('.history-list')
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireInput(inputId) {
  const root = container(inputId)
  const input = root.querySelector('.entry-input')
  const button = root.querySelector('.save-btn')

  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await save(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

function setup() {
  inputIds.forEach(wireInput)
  backendSelect.addEventListener('change', () => inputIds.forEach(refreshHistory))
  inputIds.forEach(refreshHistory)
}

setup()
