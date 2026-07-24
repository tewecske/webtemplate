interface Entry {
  inputId: string
  value: string
  createdAt: number
}

interface CreateEntryRequest {
  inputId: string
  value: string
}

const inputIds = ['note-a', 'note-b', 'note-c'] as const

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function container(inputId: string): HTMLElement {
  return document.querySelector(`[data-input-id="${inputId}"]`) as HTMLElement
}

async function save(inputId: string, value: string): Promise<void> {
  const body: CreateEntryRequest = { inputId, value }
  await fetch(`${apiBase()}/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

async function refreshHistory(inputId: string): Promise<void> {
  const res = await fetch(`${apiBase()}/entries/${inputId}`)
  const entries: Entry[] = await res.json()
  render(inputId, entries)
}

function render(inputId: string, entries: Entry[]): void {
  const list = container(inputId).querySelector('.history-list') as HTMLElement
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireInput(inputId: string): void {
  const root = container(inputId)
  const input = root.querySelector('.entry-input') as HTMLInputElement
  const button = root.querySelector('.save-btn') as HTMLButtonElement

  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await save(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

function setup(): void {
  inputIds.forEach(wireInput)
  backendSelect.addEventListener('change', () => inputIds.forEach(refreshHistory))
  inputIds.forEach(refreshHistory)
}

setup()
