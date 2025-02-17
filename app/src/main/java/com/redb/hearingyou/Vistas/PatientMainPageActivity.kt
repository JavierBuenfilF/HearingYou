package com.redb.hearingyou.Vistas

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.redb.hearingyou.DB.AppDatabase
import com.redb.hearingyou.Modelos.Firebase.PacienteFB
import com.redb.hearingyou.Modelos.Firebase.PeticionFB
import com.redb.hearingyou.Modelos.Firebase.PsicologoFavFB
import com.redb.hearingyou.R
import com.redb.hearingyou.Vistas.Adapters.PsicologosFavsAdapter


class PatientMainPageActivity : AppCompatActivity() {
    private lateinit var rvPsicologosFavs: RecyclerView

    private lateinit var consultaButton:Button
    private var flagConsulta = false
    private var database:FirebaseDatabase = FirebaseDatabase.getInstance()
    private var petitionKey = ""
    private lateinit var menubutton : ImageView

    private var psicologos: ArrayList<PsicologoFavFB> = arrayListOf()

    private val db = AppDatabase.getAppDatabase(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_main_page)

        consultaButton = findViewById(R.id.btn_consulta)
        menubutton = findViewById(R.id.menuButton)

        rvPsicologosFavs = findViewById<RecyclerView>(R.id.recyclerView_Psicologos_Favoritos).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@PatientMainPageActivity)
            adapter = PsicologosFavsAdapter(psicologos)
        }

        var Aplicacion = db.getAplicacionDao().getAplicacion()

        menubutton.setOnClickListener{
            val menu =
                arrayOf("Perfil", "Conversaciones", "Inicio", "Cerrar Sesión") // Aqui es son las opciones que contendra el menu, qui las puedes agregar i quitar
            val builder = AlertDialog.Builder(this@PatientMainPageActivity)
            builder.setTitle("Menu")
            builder.setItems(menu,{_, which ->

               when (menu[which]){       //Este es el switch donde al ser clicleado alguna opcion nos llevara a la sección adecuada
                   "Perfil" -> {
                       val intent = Intent(this, PsychologistProfileActivity::class.java)
                       intent.putExtra(EXTRA_ID_PSICOLOGO_CONVERSACION,Aplicacion.idUser)
                       intent.putExtra("EXTRA_TIPOUSUARIO",0)
                       startActivity(intent)
                   }
                   "Conversaciones"->{
                       val intent = Intent(this,ConversacionesActivity::class.java)
                       startActivity(intent)
                   }
               }
            })
            builder.show()
        }

        consultaButton.setOnClickListener{
            if(!flagConsulta){
                petitionKey = database.getReference("App").child("peticionesConsulta").push().key.toString()
                val peticion:PeticionFB = PeticionFB(Aplicacion.idUser.toString(),Aplicacion.userName.toString())
                database.getReference("App").child("peticionesConsulta").child(petitionKey.toString()).setValue(peticion)
                flagConsulta = true
                consultaButton.setText("CANCELAR")

                //AQUI VA LA PARTE DE COMENZAR A OIR PARA ABRIR LA VENTANA DE LA CONVERSACION

                val peticionReference = database.getReference("App").child("peticionesConsulta").child(petitionKey)
                peticionReference.addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            var peticion: PeticionFB? = p0.getValue(PeticionFB::class.java)
                            peticion?.id = p0.key

                            if (peticion!!.aceptada == true) {
                                val intent = Intent(
                                    this@PatientMainPageActivity,
                                    ConversacionActivity::class.java
                                )
                                intent.putExtra(EXTRA_IDCONVERSACION, peticion.idConversacion)
                                intent.putExtra(EXTRA_IDUSUARIO, Aplicacion.idUser.toString())
                                database.getReference("App").child("peticionesConsulta")
                                    .child(petitionKey).setValue(null)
                                startActivity(intent)
                            }
                        }
                    }
                })


            }
            else
            {
                database.getReference("App").child("peticionesConsulta")
                    .child(petitionKey).setValue(null)
                flagConsulta =false
                consultaButton.setText("CONSULTAR")
            }
        }
        val pacienteReference = database.getReference("App").child("pacientes").child(Aplicacion.idUser.toString())
        pacienteReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onDataChange(p0: DataSnapshot) {
                val paciente:PacienteFB? = p0.getValue(PacienteFB::class.java)

                db.getAplicacionDao().setUserName(paciente!!.sobrenombre)
                Aplicacion = db.getAplicacionDao().getAplicacion()
            }
        })

        val psicologosFavsReference = database.getReference("App").child("favoritos")
            .child(Aplicacion.idUser.toString())
        psicologosFavsReference.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val psicologoFav:PsicologoFavFB? = p0.getValue(PsicologoFavFB::class.java)
                psicologoFav?.id=p0.key

                if(psicologos.indexOf(psicologoFav)!=-1) {
                    val currentPetition = psicologos.get(psicologos.indexOf(psicologoFav))
                    currentPetition.conectado = psicologoFav!!.conectado

                    if (currentPetition.conectado == false) {
                        psicologos.remove(psicologoFav)
                    }

                    rvPsicologosFavs.adapter?.notifyDataSetChanged()
                }
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val psicologoFav:PsicologoFavFB? = p0.getValue(PsicologoFavFB::class.java)
                psicologoFav!!.id = p0.key
                psicologos.add(psicologoFav)
                rvPsicologosFavs.adapter?.notifyDataSetChanged()
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                val psicologoFav:PsicologoFavFB? = p0.getValue(PsicologoFavFB::class.java)
                psicologoFav?.id=p0.key
                psicologos.remove(psicologoFav!!)
                rvPsicologosFavs.adapter?.notifyDataSetChanged()
            }
        })
    }

}


