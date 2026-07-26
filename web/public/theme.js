// Shared by all page variants. Kept as an external script (not inlined) so the CSP on each
// page can use `script-src 'self'` without `unsafe-inline`.
(function () {
  var t = localStorage.getItem('theme')
  if (t) document.documentElement.setAttribute('data-theme', t)
})()

document.addEventListener('DOMContentLoaded', function () {
  var select = document.getElementById('theme-select')
  if (!select) return
  var current = document.documentElement.getAttribute('data-theme') || 'light'
  select.value = current
  select.addEventListener('change', function () {
    document.documentElement.setAttribute('data-theme', select.value)
    localStorage.setItem('theme', select.value)
  })
})
