package com.example.teshub_v1.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.teshub_v1.ui.publicaciones.CrearPublicacionActivity
import com.example.teshub_v1.R
import com.example.teshub_v1.ui.usuarios.PerfilFragment
import com.example.teshub_v1.ui.publicaciones.PublicacionesFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.teshub_v1.ui.eventos.EventosFragment
class HomeContainerActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_container)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Obtener referencia a la barra de búsqueda
        val searchBar: EditText = findViewById(R.id.search_bar)

        //Boton agregar publicacion
        val fab: FloatingActionButton = findViewById(R.id.fab_add_post)

        fab.setOnClickListener {
            val intent = Intent(this, CrearPublicacionActivity::class.java)
            startActivity(intent)
        }

        // Configurar la visibilidad inicial del buscador y el botón de agregar publicación
        searchBar.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is PublicacionesFragment) {
                    fragment.filter(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        //Implementar la lógica de visibilidad basada en la selección
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // En la vista de Inicio (Publicaciones), el buscador y agregar publicación debe ser VISIBLE
                    searchBar.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                    loadFragment(PublicacionesFragment())
                    true
                }
                R.id.nav_eventos -> {
                    // En la vista de Eventos, OCULTAR buscador y mostrar botón de agregar
                    searchBar.visibility = View.GONE
                    fab.visibility = View.GONE  // O View.VISIBLE si quieres el botón de crear evento
                    loadFragment(EventosFragment())
                    true
                }
                R.id.nav_profile -> {
                    // En la vista de Perfil, el buscador y el botón de agregar publicación debe ser OCULTO (View.GONE libera el espacio)
                    searchBar.visibility = View.GONE
                    fab.visibility = View.GONE
                    loadFragment(PerfilFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}