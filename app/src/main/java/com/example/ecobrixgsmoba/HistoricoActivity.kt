package com.example.ecobrixgsmoba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class HistoricoActivity : AppCompatActivity() {
    private lateinit var listaHistorico: MutableList<Pair<String, Map<String, String>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        val listaHistoricoView = findViewById<android.widget.ListView>(R.id.lista_historico)
        listaHistorico = mutableListOf()

        val bancoDados = FirebaseDatabase.getInstance().getReference("Calculos")

        bancoDados.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                listaHistorico.clear()

                for (calculoSnapshot in snapshot.children) {
                    val idCalculo = calculoSnapshot.key ?: continue
                    val calculo = calculoSnapshot.value
                    if (calculo is Map<*, *>) {
                        val calculoMapeado = calculo.entries
                            .filter { it.key is String && it.value is String }
                            .associate { it.key as String to it.value as String }

                        listaHistorico.add(idCalculo to calculoMapeado)
                    }
                }

                val adaptador = AdaptadorPersonalizado(listaHistorico, bancoDados)
                listaHistoricoView.adapter = adaptador
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@HistoricoActivity, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private class AdaptadorPersonalizado(
        private val dados: List<Pair<String, Map<String, String>>>,
        private val bancoDados: com.google.firebase.database.DatabaseReference
    ) : BaseAdapter() {
        override fun getCount(): Int = dados.size

        override fun getItem(posicao: Int): Pair<String, Map<String, String>> = dados[posicao]

        override fun getItemId(posicao: Int): Long = posicao.toLong()

        override fun getView(posicao: Int, viewConvertida: View?, parent: ViewGroup?): View {
            val view = viewConvertida ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_historico, parent, false)

            val calculo = getItem(posicao)
            val textoHistorico = view.findViewById<TextView>(R.id.texto_historico)
            val botaoExcluir = view.findViewById<ImageButton>(R.id.botao_excluir)

            textoHistorico.text = """
                Consumo: ${calculo.second["consumo"]} kWh/mês
                Dispositivo: ${calculo.second["nomeDispositivo"]}
                Horas: ${calculo.second["horas"]} horas
            """.trimIndent()

            botaoExcluir.setOnClickListener {
                bancoDados.child(calculo.first).removeValue().addOnCompleteListener { tarefa ->
                    if (tarefa.isSuccessful) {
                        Toast.makeText(view.context, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        (dados as MutableList).removeAt(posicao) // Atualiza a lista local
                        notifyDataSetChanged()
                    } else {
                        Toast.makeText(view.context, "Erro ao excluir o item.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            return view
        }
    }
}
