package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.importexport.ImportParseResult
import com.example.domain.model.AssetClass
import com.example.domain.model.OperationType
import com.example.domain.model.PortfolioItemView
import com.example.domain.model.PortfolioType
import com.example.ui.components.DataDisclaimerBanner
import com.example.ui.components.FintechCard
import com.example.ui.components.formatBrl
import com.example.ui.components.formatPct
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoValueStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestDashboardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
    state: InvestDashboardState,
    selectedType: PortfolioType,
    importFeedback: ImportParseResult?,
    pendingImportPreview: ImportParseResult?,
    onSelectType: (PortfolioType) -> Unit,
    onImportUri: (android.net.Uri) -> Unit,
    onConfirmPendingImport: () -> Unit,
    onDismissPendingImport: () -> Unit,
    onDismissImportFeedback: () -> Unit,
    onBuildTemplateCsv: () -> String,
    onBuildPortfolioCsv: (PortfolioType) -> String,
    onNotify: (String) -> Unit,
    onLoadDemoReal: () -> Unit,
    onClearPortfolio: (PortfolioType) -> Unit,
    onAddManualPosition: (PortfolioType, String, String, AssetClass, Double, Double, Double?, String, Int?) -> Unit,
    onRecordOperation: (PortfolioType, String, OperationType, Double, Double, String) -> Unit,
    onDeletePosition: (Long) -> Unit,
    onUpdateCash: (PortfolioType, Double) -> Unit
) {
    val context = LocalContext.current
    var showAddPositionDialog by remember { mutableStateOf(false) }
    var showOperationDialog by remember { mutableStateOf(false) }
    var showCashDialog by remember { mutableStateOf(false) }
    var showSpecDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportUri(uri)
    }

    val templateSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onBuildTemplateCsv().toByteArray(Charsets.UTF_8))
                }
                onNotify("Modelo padrão CSV salvo com sucesso.")
            }.onFailure {
                onNotify("Erro ao salvar modelo CSV: ${it.localizedMessage ?: "falha de escrita"}")
            }
        }
    }

    val portfolioExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onBuildPortfolioCsv(selectedType).toByteArray(Charsets.UTF_8))
                }
                onNotify("Carteira (${selectedType.label}) exportada em CSV padrão.")
            }.onFailure {
                onNotify("Erro ao exportar carteira: ${it.localizedMessage ?: "falha de escrita"}")
            }
        }
    }

    val activePositions = if (selectedType == PortfolioType.REAL) state.realPositions else state.simPositions
    val activeHhi = if (selectedType == PortfolioType.REAL) state.realHhi else state.simHhi
    val activeOps = state.operations.filter { it.portfolioType == selectedType.name }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (selectedType == PortfolioType.REAL) 0 else 1) {
            Tab(
                selected = selectedType == PortfolioType.REAL,
                onClick = { onSelectType(PortfolioType.REAL) },
                text = { Text("Carteira Real (${state.realPositions.size})") },
                modifier = Modifier.testTag("tab_real_portfolio")
            )
            Tab(
                selected = selectedType == PortfolioType.SIMULATION,
                onClick = { onSelectType(PortfolioType.SIMULATION) },
                text = { Text("Simulação (${state.simPositions.size})") },
                modifier = Modifier.testTag("tab_sim_portfolio")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DataDisclaimerBanner(
                    text = if (selectedType == PortfolioType.REAL) {
                        "Carteira Real separada da Simulação. A importação XLSX/CSV mais recente define a referência oficial. Posições sem cotação na planilha não têm preços inventados."
                    } else {
                        "Carteira de Simulação isolada da Carteira Real para testar alocações, aportes e rebalanceamentos."
                    }
                )
            }

            if (importFeedback != null) {
                item {
                    FintechCard(borderColor = ElectricCyan.copy(alpha = 0.5f)) {
                        Text("Resultado da Importação: ${importFeedback.fileName}", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        Text("${importFeedback.positions.size} posições importadas (${importFeedback.fileType}).", style = MaterialTheme.typography.bodySmall)
                        importFeedback.warnings.take(4).forEach { w ->
                            Text("• $w", style = MaterialTheme.typography.bodySmall, color = AmberGold)
                        }
                        TextButton(onClick = onDismissImportFeedback) { Text("Dispensar") }
                    }
                }
            }

            item {
                FintechCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Patrimônio (${selectedType.label})", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Text(formatBrl(activeHhi.totalEquityBrl), style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize))
                            Text("Investido: ${formatBrl(activeHhi.totalInvestedBrl)} • Caixa: ${formatBrl(activeHhi.cashBrl)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        TextButton(onClick = { showCashDialog = true }, modifier = Modifier.testTag("edit_cash_button")) {
                            Text("Editar Caixa")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedType == PortfolioType.REAL) {
                            Button(
                                onClick = {
                                    filePicker.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/plain",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("import_spreadsheet_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Importar XLSX/CSV")
                            }
                        }
                        OutlinedButton(
                            onClick = { showAddPositionDialog = true },
                            modifier = Modifier.weight(1f).testTag("add_position_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nova Posição")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { templateSaver.launch("invest_ai_modelo_carteira.csv") },
                            modifier = Modifier.weight(1f).testTag("download_csv_template_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Modelo CSV")
                        }
                        OutlinedButton(
                            onClick = { showSpecDialog = true },
                            modifier = Modifier.weight(1f).testTag("spreadsheet_spec_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Padrão Colunas")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showOperationDialog = true },
                            modifier = Modifier.weight(1f).testTag("record_operation_button")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Operação / Provento")
                        }
                        if (activePositions.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val prefix = if (selectedType == PortfolioType.REAL) "real" else "simulacao"
                                    portfolioExporter.launch("invest_ai_carteira_$prefix.csv")
                                },
                                modifier = Modifier.weight(1f).testTag("export_portfolio_csv_button")
                            ) {
                                Text("Exportar CSV")
                            }
                        } else if (selectedType == PortfolioType.REAL) {
                            OutlinedButton(
                                onClick = onLoadDemoReal,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Demo Fictícia")
                            }
                        }
                    }

                    if (activePositions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onClearPortfolio(selectedType) }) {
                                Text("Limpar ${selectedType.label}", color = CoralRed)
                            }
                        }
                    }
                }
            }

            if (activePositions.isEmpty()) {
                item {
                    FintechCard {
                        Text(
                            "Nenhuma posição cadastrada em ${selectedType.label}.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Formato CSV/XLSX aceito: colunas Ticker, Nome, Classe, Quantidade, Preço Médio, Preço Atual (opcional), Setor (opcional), Data-Com (opcional) e linha CAIXA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(activePositions, key = { it.id }) { pos ->
                    PositionCard(pos = pos, onDelete = { onDeletePosition(pos.id) })
                }
            }

            if (activeOps.isNotEmpty()) {
                item {
                    FintechCard {
                        Text("Operações e Proventos Recentes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        activeOps.take(8).forEach { op ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${op.ticker} • ${op.operationType}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${op.dateIso} • ${op.notes}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(formatBrl(op.totalValue), style = MonoValueStyle, color = EmeraldPrimary)
                            }
                        }
                    }
                }
            }

            if (selectedType == PortfolioType.REAL && state.importHistory.isNotEmpty()) {
                item {
                    FintechCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Histórico de Importações", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                        state.importHistory.forEach { h ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        h.fileName + if (h.isActiveRealReference) " [REFERÊNCIA ATUAL]" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (h.isActiveRealReference) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${h.fileType} • ${h.recordCount} ativos • ${fmt.format(Date(h.importedAt))}" +
                                            if (h.isDemoData) " • FICTÍCIO" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingImportPreview != null) {
        ImportPreviewDialog(
            preview = pendingImportPreview,
            onDismiss = onDismissPendingImport,
            onConfirm = onConfirmPendingImport
        )
    }

    if (showSpecDialog) {
        ImportStandardSpecDialog(
            onDismiss = { showSpecDialog = false },
            onDownloadTemplate = { templateSaver.launch("invest_ai_modelo_carteira.csv") }
        )
    }

    if (showAddPositionDialog) {
        AddPositionDialog(
            portfolioType = selectedType,
            onDismiss = { showAddPositionDialog = false },
            onConfirm = { ticker, name, cls, qty, pm, cur, sec, dc ->
                onAddManualPosition(selectedType, ticker, name, cls, qty, pm, cur, sec, dc)
                showAddPositionDialog = false
            }
        )
    }

    if (showOperationDialog) {
        RecordOperationDialog(
            onDismiss = { showOperationDialog = false },
            onConfirm = { ticker, opType, qty, price, notes ->
                onRecordOperation(selectedType, ticker, opType, qty, price, notes)
                showOperationDialog = false
            }
        )
    }

    if (showCashDialog) {
        EditCashDialog(
            currentCash = activeHhi.cashBrl,
            onDismiss = { showCashDialog = false },
            onConfirm = { newVal ->
                onUpdateCash(selectedType, newVal)
                showCashDialog = false
            }
        )
    }
}

@Composable
private fun PositionCard(pos: PortfolioItemView, onDelete: () -> Unit) {
    FintechCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pos.ticker + if (pos.isDemo) " [DEMO]" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldPrimary
                )
                Text("${pos.name} • ${pos.assetClass.shortLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Fonte: ${pos.dataSourceLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remover ${pos.ticker}", tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Qtd / PM", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("${"%.2f".format(pos.quantity)} x ${formatBrl(pos.averagePrice)}", style = MonoValueStyle)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total / Var.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(formatBrl(pos.totalCurrentBrl), style = MonoValueStyle)
                if (pos.hasMarketPrice) {
                    Text(
                        formatPct(pos.profitLossPercent),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pos.profitLossPercent >= 0) EmeraldPrimary else CoralRed
                    )
                } else {
                    Text("Cotação não informada", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                }
            }
        }
    }
}
