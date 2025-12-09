package com.example.teshub_v1.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.teshub_v1.R
import com.example.teshub_v1.ui.eventos.EventosFragment
import com.example.teshub_v1.ui.publicaciones.CrearPublicacionActivity
import com.example.teshub_v1.ui.publicaciones.PublicacionesFragment
import com.example.teshub_v1.ui.usuarios.AsesorDashboardFragment
import com.example.teshub_v1.ui.usuarios.ConnectionsFragment
import com.example.teshub_v1.ui.usuarios.PerfilFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeContainerActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    private var lastSelectedItemId: Int = R.id.nav_home
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_container)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val searchBar: EditText = findViewById(R.id.search_bar)
        val fab: FloatingActionButton = findViewById(R.id.fab_add_post)

        val sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val rol = sharedPref.getString("rol", "") ?: ""
        val isAdvisor = rol.equals("Asesor", ignoreCase = true) || rol == "2"

        if (isAdvisor) {
            bottomNavigationView.menu.findItem(R.id.nav_connections)?.title = "Dashboard"
        }

        fab.setOnClickListener {
            startActivity(Intent(this, CrearPublicacionActivity::class.java))
        }

        // --- LÓGICA DE BÚSQUEDA ---
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancelar búsqueda anterior si el usuario sigue escribiendo
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Debounce: Esperar 600ms después de escribir para buscar
                searchRunnable = Runnable {
                    if (query.isNotEmpty()) {
                        // Si hay texto, mostramos el fragmento de resultados
                        mostrarResultadosBusqueda(query)
                        // Ocultamos el FAB y BottomNav para dar foco a los resultados (opcional)
                        // fab.visibility = View.GONE
                    } else {
                        // Si borra el texto, restauramos el fragmento original
                        restaurarFragmentoOriginal(bottomNavigationView.selectedItemId)
                        fab.visibility = View.VISIBLE
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 600)
            }
        })

        // Buscar al presionar Enter en el teclado
        searchBar.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchBar.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchRunnable?.let { searchHandler.removeCallbacks(it) } // Cancelar timer
                    mostrarResultadosBusqueda(query)
                    ocultarTeclado()
                }
                true
            } else {
                false
            }
        }

        // --- NAVEGACIÓN ---
        bottomNavigationView.setOnItemSelectedListener { item ->
            // Si cambian de pestaña, limpiamos la búsqueda
            if (searchBar.text.isNotEmpty()) {
                searchBar.text.clear()
                ocultarTeclado()
            }

            lastSelectedItemId = item.itemId // Guardar selección

            when (item.itemId) {
                R.id.nav_home -> {
                    searchBar.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                    loadFragment(PublicacionesFragment())
                    true
                }
                R.id.nav_eventos -> {
                    searchBar.visibility = View.GONE
                    fab.visibility = View.GONE // O Visible si quieres crear eventos
                    loadFragment(EventosFragment())
                    true
                }
                R.id.nav_connections -> {
                    searchBar.visibility = View.GONE
                    fab.visibility = View.GONE
                    if (isAdvisor) loadFragment(AsesorDashboardFragment()) else loadFragment(ConnectionsFragment())
                    true
                }
                R.id.nav_profile -> {
                    searchBar.visibility = View.GONE
                    fab.visibility = View.GONE
                    loadFragment(PerfilFragment())
                    true
                }
                else -> false
            }
        }

        // Carga inicial
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

    private fun mostrarResultadosBusqueda(query: String) {
        // Evitar recargar si ya estamos buscando lo mismo (opcional)
        val fragment = BusquedaFragment.newInstance(query)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun restaurarFragmentoOriginal(itemId: Int) {
        // Simplemente "reselecciona" el item actual para recargar su fragmento
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = itemId
    }

    private fun ocultarTeclado() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}