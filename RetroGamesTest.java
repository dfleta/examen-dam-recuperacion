
import io.quarkus.test.junit.QuarkusTest;

/**
 * Construye una aplicacion que maneja una base de datos
 * de un servicio para jugar a videojuegos retro,
 * con las personas usuarias (users) del servicio
 * y los retrogames disponibles (items).
 * Las usuarias realizan peticiones (ordenes) al servicio
 * para jugar a juegos. 
 */

@QuarkusTest
public class RetroGamesTest {

    @PersistenceContext
	private EntityManager em;

	// @Inject
	// Repositorio repo;

	/**
	 * Tests sobre los mappings
	 * 
	 * Observa el esquema de la base de datos que espera 
	 * la aplicacion en el fichero:
	 * src/main/resources/schema.sql
	 */
	
	// Completa la definicion y el mapping
	// de la clase RetroGame a la tabla t_items

	@Test
	public void test_mapping_game() {
		RetroGame game = em.find(RetroGame.class, "El dia del tentaculo");
		assertNotNull(game);
		assertEquals("El dia del tentaculo", game.getNombre());
		assertEquals(7, game.getDificultad(), 0); //item_prop
		assertEquals("RetroGame", game.getTipo());
	}
	

	// Completa la definicion y el mapping
	// de la clase Usuaria a la tabla t_users
	@Test
	public void test_mapping_user() {
		Usuaria guy = em.find(Usuaria.class, "Guybrush");
		assertNotNull(guy);
		assertEquals("Guybrush", guy.getNombre());
		assertEquals(15, guy.getDestreza(), 0);  //user_prop
	}

	// Completa la definicion y el mapping
	// de la clase Orden a la tabla t_ordenes
	// El id de esta clase ha de seguir una estrategia
	// Identity
	@Test 
	public void test_mapping_orden() {
		Orden pedido = em.find(Orden.class, 1L);
		assertNotNull(pedido);
		assertEquals("Guybrush", pedido.getUser().getNombre());
		assertEquals("El dia del tentaculo", pedido.getItem().getNombre());
		}
	
	/**
	 * Crea una clase llamada Repositorio,
	 * anotala con @ApplicationScoped
	 * e inyectala en los casos test
	 * 
	 * @Inject
	 * Repositorio repo;
	 */
	@Test
	public void test_repositorio_es_componente() {
		assertNotNull(repo);
	}

	/**
	 * Implementa el metodo cargaUser del repositorio
	 * que devuelve la usuaria con el nombre indicado
	 */
	@Test
	public void test_carga_usuaria() {
		assertNotNull(repo);
		Usuaria guy = repo.cargaUsuaria("Guybrush");
		assertNotNull(guy);
		assertEquals("Guybrush", guy.getNombre());
		assertEquals(15, guy.getDestreza());
	}

	/**
	 * Implementa el metodo cargaItem del repositorio
	 * que devuelve el item (retro game) con el nombre indicado
	 */
	@Test
	public void test_carga_item() {
		assertNotNull(repo);
		RetroGame item = (RetroGame) repo.cargaItem("El dia del tentaculo");
		assertNotNull(item);
		assertEquals("El dia del tentaculo", item.getNombre());
		assertEquals(7, item.getDificultad(), 0);
	}

	/**
     * Implementa el metodo ordenar del repositorio
	 * que permite a una usuaria pedir un item (RetroGame).
     * La usuaria y el item ya existen en la bbdd (NO has de crearlos).
	 * 
     * El metodo devuelve la orden de tipo Orden creada.
     * 
     * Guarda la orden en la base de datos.
	 */
	@Test
	@Transactional
	public void test_ordenar_ok() {
		assertNotNull(repo);
		Optional<Orden> orden = repo.ordenar("Bernard Bernoulli", "Ghosts n Goblins");

		// test logica del metodo
		assertTrue(orden.isPresent());
		assertNotNull(orden.get());
		assertNotNull(orden.get().getId());
		assertEquals("Bernard Bernoulli", orden.get().getUser().getNombre());
		assertEquals("Ghosts n Goblins", orden.get().getItem().getNombre());

		// test del efecto en la bbdd
		em.flush();
		Orden rollback = em.find(Orden.class, orden.get().getId());
		assertNotNull(rollback);
		// rollback de la bbdd
		em.remove(rollback);
	}

	/**
     * Modifica el metodo ordenar del repositorio
	 * para que no permita generar ordenes de RetroGames
	 * si no existe la usuaria en la base de datos.
	 */
	@Test
	@Transactional
	public void test_ordenar_no_usuaria() {
		assertNotNull(repo);
		Optional<Orden> orden = repo.ordenar("Wilson", "Ghosts n Goblins");
		assertFalse(orden.isPresent());
	}

	/**
     * Modifica el metodo ordenar del repositorio
	 * para que no permita generar ordenes de RetroGames
	 * si no existe el RetroGame en la base de datos.
	 */
	@Test
	@Transactional
	public void test_ordenar_no_game() {
		assertNotNull(repo);
		Optional<Orden> orden = repo.ordenar("Bernard Bernoulli", "Monkey Island");
		assertFalse(orden.isPresent());
	}

	/**
	 * Modifica el metodo ordenar para que lance una excepcion
	 * del tipo NotEnoughProException
	 * cuando la destreza de la usuaria sea menor
	 * a la dificultad del RetroGame.
	 */
	@Test
	public void test_ordenar_usuaria_no_pro() throws NotEnoughProException {
		assertNotNull(repo);
		assertThrows(NotEnoughProException.class, () -> repo.ordenar("Guybrush", "Ghosts n Goblins"), "La usuaria no es pro"); 
	}

	/**
	 * Implementa el metodo ordenarMultiple para que una usuaria
	 * pueda ordenar más de un RetroGame a la vez.
	 * Guarda las ordenes en la base de datos.
     * 
     * No se permiten ordenes si la usuaria no existe en la base de datos.
	 * 
	 * No se ordenan items que no existen en la base de datos.
	 * 
	 * No se permiten ordenes si la usuaria no posee más
	 * destreza de la que requiere el retrogame.
	 */
	@Test
	@Transactional
	public void test_ordenar_multiples_items() {
		assertNotNull(repo);

		// test logica del metodo
		// no se permiten ordenes si el usuario no existe en la base de datos
		List<Orden> ordenes = repo.ordenarMultiple("Wilson", Arrays.asList("Ghosts n Goblins", "El dia del tentaculo"));
		assertTrue(ordenes.isEmpty());

		// test efecto en la base de datos:
		// query para obtener todas las ordenes de Bernoulli
		// de manera agnostica al patron DAO /Active record
		TypedQuery<Orden> query = em.createQuery("select orden from Orden orden join orden.user user where user.nombre = 'Wilson'", Orden.class);
		List<Orden> pedidos = query.getResultList();
		assertTrue(pedidos.isEmpty());		

		// no se ordenan items que no existen en la base de datos
		ordenes = repo.ordenarMultiple("Arthur", Arrays.asList("Ghosts n Goblins", "Monkey Island"));
		assertEquals(1, ordenes.size());
		query = em.createQuery("select orden from Orden orden join orden.user user where user.nombre = 'Arthur'", Orden.class);
		pedidos = query.getResultList();
		assertEquals(1, pedidos.size());
		assertEquals("Arthur", pedidos.get(0).getUser().getNombre());
		assertEquals("Ghosts n Goblins", pedidos.get(0).getItem().getNombre());
	}

	/**
     * Implementa el metodo deleteUser() del repositorio
	 * que elimina la usuaria indicada en la base de datos.
     */
	@Test
	@Transactional
	public void test_delete_user() {
		assertNotNull(repo);

		// Usuaria eliminada porque no existe en ordenes => no rompe integridad referencial
		assertTrue(repo.deleteUser("Nick and Tom"));
		// Si no existe la usuaria
		assertFalse(repo.deleteUser("Indiana Jones"));

		// chequeo el efecto sobre la base de datos
		em.flush();
		Usuaria user = em.find(Usuaria.class, "Nick and Tom");
		assertNull(user);
		List<Usuaria> usuarias = em.createQuery("from Usuaria", Usuaria.class).getResultList();
		assertEquals(4, usuarias.size());

		// Si la usuaria ha ejecutado ordenes
		// se elimina la usuaria
		// y sus ordenes
		assertTrue(repo.deleteUser("Donatello"));
		em.flush();
		user = em.find(Usuaria.class, "Donatello");
		assertNull(user);
		Orden orden = em.find(Orden.class, 2L);
		assertNull(orden);
	} 
}