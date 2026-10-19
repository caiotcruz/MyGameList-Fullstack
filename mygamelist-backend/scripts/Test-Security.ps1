param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
$uri=[Uri]$BaseUrl
if (-not $uri.IsLoopback -or $uri.Scheme -notin @('http','https')) { throw 'Este roteiro aceita somente API local.' }
Add-Type -AssemblyName System.Net.Http
$handler=New-Object System.Net.Http.HttpClientHandler
$handler.UseProxy=$false
$client=New-Object System.Net.Http.HttpClient($handler)
$client.Timeout=[TimeSpan]::FromSeconds(10)
function Probe([string]$method,[string]$path,[string]$body,[string]$type='application/json') {
    $request=New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::new($method),($BaseUrl.TrimEnd('/')+$path))
    if ($method -ne 'GET' -and $null -ne $body) { $request.Content=New-Object System.Net.Http.StringContent($body,[Text.Encoding]::UTF8,$type) }
    try {
        $response=$client.SendAsync($request).GetAwaiter().GetResult()
        try { return [int]$response.StatusCode } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}
try {
    # Small, sequential functional probes; this is NOT a stress test.
    if ((Probe 'GET' '/users/1' $null) -ne 401) { throw 'A rota privada deveria retornar 401.' }
    if ((Probe 'POST' '/auth/login' '{"email":"a","email":"b"}') -ne 400) { throw 'JSON duplicado nao foi rejeitado.' }
    if ((Probe 'POST' '/auth/login' ('x'*393217)) -ne 413) { throw 'Corpo excessivo nao foi rejeitado.' }
    if ((Probe 'POST' '/auth/login' '{}' 'text/plain') -ne 415) { throw 'Tipo de conteudo invalido nao foi rejeitado.' }
    $identity=[Guid]::NewGuid().ToString()+'@example.invalid'
    $body=@{email=$identity;password='incorrect-local-probe'} | ConvertTo-Json -Compress
    $limited=$false
    for ($i=0;$i -lt 17;$i++) {
        $status=Probe 'POST' '/auth/login' $body
        if ($status -eq 429) { $limited=$true;break }
        if ($status -ne 401) { throw "Status inesperado no teste de autenticacao: $status" }
    }
    if (-not $limited) { throw 'O limite de autenticacao nao foi observado.' }
    Write-Host 'Protecao de rotas, JSON, tamanho, content-type e limite de login validados.'
    Write-Host 'O teste consome o limite de login deste IP; aguarde ate 60 segundos antes de entrar normalmente.'
} finally { $client.Dispose();$handler.Dispose() }
