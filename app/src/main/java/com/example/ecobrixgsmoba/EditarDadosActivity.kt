package com.example.ecobrixgsmoba

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class EditarDadosActivity : AppCompatActivity() {
    private lateinit var bancoDados: DatabaseReference
    private lateinit var listaEquipamentos: ListView
    private lateinit var containerEdicao: LinearLayout
    private lateinit var campoPotencia: EditText
    private lateinit var campoHorasUso: EditText
    private lateinit var campoDiasUso: EditText
    private lateinit var campoConsumo: TextView
    private lateinit var botaoSalvar: Button
    private lateinit var botaoCancelar: Button
    private var idSelecionado: String? = null
    private val listaDados = mutableListOf<Pair<String, Map<String, String>>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_dados)

        bancoDados = FirebaseDatabase.getInstance().getReference("Equipamentos")
        listaEquipamentos = findViewById(R.id.lista_equipamentos)
        containerEdicao = findViewById(R.id.container_edicao)
        campoPotencia = findViewById(R.id.campo_potencia)
        campoHorasUso = findViewById(R.id.campo_horas_uso)
        campoDiasUso = findViewById(R.id.campo_dias_uso)
        campoConsumo = findViewById(R.id.campo_consumo)
        botaoSalvar = findViewById(R.id.botao_salvar)
        botaoCancelar = findViewById(R.id.botao_cancelar)

        carregarDados()

        botaoSalvar.setOnClickListener { salvarAlteracoes() }
        botaoCancelar.setOnClickListener { cancelarEdicao() }

        listaEquipamentos.setOnItemClickListener { _, _, posicao, _ ->
            try {
                val dadoSelecionado = listaDados[posicao]
                idSelecionado = dadoSelecionado.first
                preencherCamposEdicao(dadoSelecionado.second)
                alternarContainerEdicao(true)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao selecionar o item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Corrigido: Usando TextWatcher para recalcular o consumo
        campoPotencia.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularConsumo()
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        campoHorasUso.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularConsumo()
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        campoDiasUso.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularConsumo()
            }

            override fun afterTextChanged(editable: Editable?) {}
        })
    }

    private fun carregarDados() {
        bancoDados.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaDados.clear()

                for (dadoSnapshot in snapshot.children) {
                    val id = dadoSnapshot.key ?: continue
                    val dados = dadoSnapshot.value as? Map<String, String> ?: continue
                    listaDados.add(id to dados)
                }

                val adaptador = ArrayAdapter(
                    this@EditarDadosActivity,
                    android.R.layout.simple_list_item_1,
                    listaDados.map {
                        "Equipamento: ${it.second["nome"]}, Consumo Médio: ${it.second["consumo"]} kWh/mês"
                    }
                )
                listaEquipamentos.adapter = adaptador
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditarDadosActivity, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun preencherCamposEdicao(dados: Map<String, String>) {
        campoPotencia.setText(dados["potencia"] ?: "")
        campoHorasUso.setText(dados["horasUso"] ?: "")
        campoDiasUso.setText(dados["diasUso"] ?: "")
        campoConsumo.text = dados["consumo"] ?: "0.00 kWh/mês"
    }

    private fun salvarAlteracoes() {
        val potencia = campoPotencia.text.toString().toDoubleOrNull() ?: 0.0
        val horasUso = campoHorasUso.text.toString().toDoubleOrNull() ?: 0.0
        val diasUso = campoDiasUso.text.toString().toDoubleOrNull() ?: 0.0

        if (potencia <= 0 || horasUso <= 0 || diasUso <= 0) {
            Toast.makeText(this, "Por favor, preencha todos os campos com valores válidos.", Toast.LENGTH_SHORT).show()
            return
        }

        val consumo = (potencia * horasUso * diasUso) / 1000

        val dadosAtualizados = mapOf(
            "potencia" to potencia.toString(),
            "horasUso" to horasUso.toString(),
            "diasUso" to diasUso.toString(),
            "consumo" to String.format("%.2f", consumo)
        )

        idSelecionado?.let { id ->
            bancoDados.child(id).updateChildren(dadosAtualizados).addOnCompleteListener { tarefa ->
                if (tarefa.isSuccessful) {
                    Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                    alternarContainerEdicao(false)
                } else {
                    Toast.makeText(this, "Erro ao atualizar os dados.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cancelarEdicao() {
        alternarContainerEdicao(false)
    }

    private fun alternarContainerEdicao(mostrar: Boolean) {
        containerEdicao.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    // Função para recalcular o consumo sempre que os campos forem alterados
    private fun recalcularConsumo() {
        val potencia = campoPotencia.text.toString().toDoubleOrNull() ?: 0.0
        val horasUso = campoHorasUso.text.toString().toDoubleOrNull() ?: 0.0
        val diasUso = campoDiasUso.text.toString().toDoubleOrNull() ?: 0.0

        if (potencia > 0 && horasUso > 0 && diasUso > 0) {
            val consumo = (potencia * horasUso * diasUso) / 1000
            campoConsumo.text = "Consumo Estimado: ${String.format("%.2f", consumo)} kWh/mês"
        } else {
            campoConsumo.text = "Consumo Estimado: 0.00 kWh/mês"
        }
    }
}
