const HEADERS = ['ID','Tanggal','Pelanggan','WhatsApp','Produk','Qty','Total','DP','Sisa','Status'];

function setup() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sh = ss.getSheetByName('Orders') || ss.insertSheet('Orders');
  if (sh.getLastRow() === 0) sh.appendRow(HEADERS);
  return ContentService.createTextOutput('SukmaGrafika siap');
}

function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents || '{}');
    if (d.action === 'createOrder') return json(createOrder(d));
    return json({ok:false,error:'Unknown action'});
  } catch (err) {
    return json({ok:false,error:String(err)});
  }
}

function doGet(e) {
  const action = e.parameter.action || 'orders';
  if (action === 'orders') return json(getOrders());
  return json({ok:false,error:'Unknown action'});
}

function createOrder(d) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sh = ss.getSheetByName('Orders') || ss.insertSheet('Orders');
  if (sh.getLastRow() === 0) sh.appendRow(HEADERS);

  const now = new Date();
  const tz = Session.getScriptTimeZone();
  const id = 'SG-' + Utilities.formatDate(now, tz, 'yyyyMMdd-HHmmss');
  const total = Number(d.total || 0);
  const dp = Number(d.dp || 0);

  sh.appendRow([
    id, now, d.customer || '', d.whatsapp || '', d.product || '',
    Number(d.qty || 0), total, dp, total - dp, d.status || 'Baru'
  ]);

  return {ok:true,id:id};
}

function getOrders() {
  const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Orders');
  if (!sh) return {ok:true,data:[]};
  return {ok:true,data:sh.getDataRange().getValues()};
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
