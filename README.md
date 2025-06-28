# Coletor de Dados (V3 MVP)

Este sistema coleta periodicamente:

- Dados do giroscópio
- Localização GPS (latitude e longitude)
- Foto da câmera frontal
- Data/hora e ID do dispositivo

As coletas são armazenadas localmente e enviadas para um servidor via API REST.

---

## 🛠️ Recuperação de Falhas

- **Sem Internet ou erro na API:**  
  As coletas são salvas localmente no banco de dados e **tentadas novamente** no próximo ciclo ou reinício do serviço.

- **Localização nula ou zerada (0.0, 0.0):**  
  A coleta é descartada e o usuário é notificado por Toast (`Falha: localização indisponível`).

- **Giroscópio ainda não disponível:**  
  A coleta também é descartada e notificada (`Erro: giroscópio ainda não disponível`).

- **Erro inesperado:**  
  É exibido um Toast e uma notificação no topo do celular (via `NotificationManager`).

---

## 🌀 Serviço em segundo plano

O `ColetaService` roda como serviço em foreground com notificação ativa, mantendo o sistema vivo mesmo com a tela desligada.

---

## ⏱️ Intervalo de coleta

- Padrão: a cada 10 segundos
- Pode ser alterado dinamicamente via Intent `ACTION_UPDATE_INTERVAL`

---

## 🔁 Reenvio automático

Ao iniciar o serviço, ele **verifica coletas não enviadas** e tenta reenviar todas para o backend.
