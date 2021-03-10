package se.ju.student.hitech.news

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import se.ju.student.hitech.events.Event
import se.ju.student.hitech.news.Novelty
import kotlin.collections.List as List

var newsRepository = NewsRepository()

class NewsRepository {

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var newsList = mutableListOf<Novelty>()

    fun addNovelty(title: String, content: String) {

        sortNewsList()
        val novelty = HashMap<String, Any>()
        novelty["title"] = title
        novelty["content"] = content
        novelty["id"] = when {
            newsList.count() == 0 -> 1
            else -> newsList.first().id + 1
        }

        db.collection("news").document(novelty["id"].toString()).set(novelty)
    }

    fun getAllNews(): List<Novelty> {
        return newsList
    }

    fun loadChangesInNewsData() {
        db.collection("news").orderBy("id").addSnapshotListener { snapshot, e ->

            if (e != null) {
                Log.w(TAG, "Failed to load news", e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                for (dc in snapshot!!.documentChanges) {
                    val novelty = dc.document.toObject(Novelty::class.java)
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> added(novelty)
                        DocumentChange.Type.MODIFIED -> modified(novelty)
                        DocumentChange.Type.REMOVED -> removed(novelty)
                    }
                }
            }
        }
    }

    private fun added(novelty: Novelty) {
        if (!newsList.contains(novelty)) {
            newsList.add(novelty)
        }
    }

    private fun modified(novelty: Novelty) {
        // remove old
        // add new
    }

    private fun removed(novelty: Novelty) {
        if (newsList.contains(novelty)) {
            newsList.remove(novelty)
        }
    }

    fun deleteNovelty(id: Int) {
        db.collection("news").document(id.toString()).delete()
    }

    private fun sortNewsList() {
        newsList.sortByDescending { novelty ->
            novelty.id
        }
    }

    fun updateNovelty() {
        // TODO
    }

    fun getNoveltyById(id: Int): Novelty? {

        for (novelty in newsList) {
            if (novelty.id == id) {
                return novelty
            }
        }
        return null
    }
}
