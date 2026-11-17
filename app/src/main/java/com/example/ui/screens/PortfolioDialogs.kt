package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.importexport.ImportParseResult
import com.example.data.importexport.SpreadsheetImporter
import com.example.domain.model.AssetClass
import com.example.domain.model.OperationType
import com.example.domain.model.PortfolioType
import com.example.ui.components.formatBrl
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextSecondary

@Composable
fun ImportPreviewDialog(
    preview: ImportParseResult,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Pré-visualização da Importação", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${preview.fileName} • Formato ${preview.fileType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Ao confirmar, esta importação substituirá a Carteira Real atual e será gravada como a referência oficial mais recente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${preview.positions.size} ativos válidos", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                    preview.cashBalanceBrl?.let {
                        Text("Caixa: ${formatBrl(it)}", style = MonoValueStyle, color = ElectricCyan)
                    }
                }

                if (preview.detectedColumns.isNotEmpty()) {
                    Text(
                        "Colunas: ${preview.detectedColumns.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                if (preview.warnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmberGold.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Alertas de Auditoria (${preview.warnings.size})", style = MaterialTheme.typography.labelMedium, color = AmberGold)
                        preview.warnings.take(5).forEach { w ->
                            Text("• $w", style = MaterialTheme.typography.bodySmall, color = AmberGold)
                        }
                        if (preview.warnings.size > 5) {
                            Text("... e mais ${preview.warnings.size - 5} alertas.", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                        }
                    }
                }

                HorizontalDivider(color = SubtleBorder)

                preview.positions.forEach { pos ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardElevated.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${pos.ticker} • ${pos.assetClass}", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                            Text(
                                "${"%.2f".format(pos.quantity)} x PM ${formatBrl(pos.averagePrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (pos.hasMarketPrice) {
                                Text(formatBrl(pos.currentPrice), style = MonoValueStyle)
                                Text("Cotação ok", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                            } else {
                                Text("Sem cotação", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_import_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
            ) {
                Text("Confirmar na Carteira Real")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ImportStandardSpecDialog(
    onDismiss: () -> Unit,
    onDownloadTemplate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Padrão de Planilha CSV / XLSX") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Arquivos .csv (UTF-8 ou Windows-1252, separador ';' ou ',') e .xlsx são processados localmente. Valores aceitam formato brasileiro (ex: 1.234,56).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                SpreadsheetImporter.STANDARD_COLUMN_SPECS.forEach { spec ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardElevated.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(spec.name, style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                            Text(
                                if (spec.required) "OBRIGATÓRIA" else "OPCIONAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (spec.required) AmberGold else ElectricCyan
                            )
                        }
                        Text("Aceita: ${spec.aliases}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Exemplo: ${spec.example}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text(
                    "Regra de integridade: linhas com Ticker = CAIXA atualizam o saldo em caixa. Cotações ausentes permanecem vazias e nunca são inventadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberGold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onDownloadTemplate()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
            ) {
                Text("Salvar Modelo CSV")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPositionDialog(
    portfolioType: PortfolioType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, AssetClass, Double, Double, Double?, String, Int?) -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(AssetClass.ACOES_BR) }
    var expanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("100") }
    var pmText by remember { mutableStateOf("") }
    var curText by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var dataComText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Posição • ${portfolioType.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = ticker, onValueChange = { ticker = it }, label = { Text("Ticker (ex: ITUB4)") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome (opcional)") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedClass.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Classe") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AssetClass.entries.forEach { cls ->
                            DropdownMenuItem(text = { Text(cls.label) }, onClick = { selectedClass = cls; expanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText, onValueChange = { qtyText = it }, label = { Text("Qtd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = pmText, onValueChange = { pmText = it }, label = { Text("Preço Médio") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = curText, onValueChange = { curText = it }, label = { Text("Preço Atual (opc.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = dataComText, onValueChange = { dataComText = it }, label = { Text("Dia Data-Com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                OutlinedTextField(value = sector, onValueChange = { sector = it }, label = { Text("Setor / Segmento") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = qtyText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val pm = pmText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val cur = curText.replace(",", ".").toDoubleOrNull()
                val dc = dataComText.toIntOrNull()?.takeIf { it in 1..31 }
                if (ticker.isNotBlank() && qty > 0 && pm > 0) {
                    onConfirm(ticker, name, selectedClass, qty, pm, cur, sector, dc)
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordOperationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, OperationType, Double, Double, String) -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var opType by remember { mutableStateOf(OperationType.PROVENTO) }
    var expanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Operação / Provento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = opType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OperationType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { opType = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = ticker, onValueChange = { ticker = it }, label = { Text("Ativo / Ticker") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Qtd") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Valor Unit. (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observação") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = qtyText.replace(",", ".").toDoubleOrNull() ?: 1.0
                val p = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (p > 0) onConfirm(ticker, opType, q, p, notes)
            }) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun EditCashDialog(
    currentCash: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf("%.2f".format(currentCash)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar Saldo em Caixa (R$)") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Saldo em R$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                text.replace(",", ".").toDoubleOrNull()?.let { onConfirm(it) }
            }) { Text("Atualizar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
