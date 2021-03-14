package se.ju.student.hitech.news

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import se.ju.student.hitech.*
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_CREATE_NEWS_POST
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_NEWS
import se.ju.student.hitech.news.ViewNoveltyActivity.Companion.EXTRA_NOVELTY_ID
import se.ju.student.hitech.databinding.CardNewsBinding
import se.ju.student.hitech.databinding.FragmentNewsBinding
import se.ju.student.hitech.news.NewsRepository.Companion.newsRepository
import se.ju.student.hitech.user.UserRepository

class NewsFragment : Fragment() {

    lateinit var binding: FragmentNewsBinding
    private val viewModel: NewsViewModel by viewModels()
    var userRepository = UserRepository()

    companion object {
        fun newInstance() = NewsFragment()
        var loggedIn: Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentNewsBinding.inflate(layoutInflater, container, false).run {
        binding = this
        root
    }

    override fun onStart() {
        super.onStart()
        if (userRepository.checkIfLoggedIn()) {
            loggedIn = true
            binding.fabCreateNewPost.visibility = VISIBLE
        } else {
            binding.fabCreateNewPost.visibility = GONE
            loggedIn = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            registerForContextMenu(this)
        }

        viewModel.news.observe(viewLifecycleOwner) {

            if (it != null) {
                binding.rvRecyclerView.post {
                    binding.rvRecyclerView.apply {
                        adapter = NewsAdapter(it)
                        adapter?.notifyDataSetChanged()
                    }
                    binding.progressBar.visibility = GONE
                }
            }
        }

        binding.swipeRefreshNews.setOnRefreshListener {
            binding.swipeRefreshNews.isRefreshing = false
        }

        binding.fabCreateNewPost.setOnClickListener {
            (context as MainActivity).changeToFragment(TAG_FRAGMENT_CREATE_NEWS_POST)
        }
    }

    class NewsViewModel : ViewModel() {
        var news = MutableLiveData<List<Novelty>>()

        init {
            newsRepository.loadChangesInNewsData()
            val fetchedNews = newsRepository.getAllNews()
            news.postValue(fetchedNews)
        }
    }


    class NewsViewHolder(val binding: CardNewsBinding) : RecyclerView.ViewHolder(binding.root)

    class NewsAdapter(val news: List<Novelty>) : RecyclerView.Adapter<NewsViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NewsViewHolder(
            CardNewsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
            val novelty = news[position]
            val id = novelty.id

            holder.binding.newsTitleNoImage.text = novelty.title
            holder.binding.cardNews.setOnClickListener {

                holder.binding.cardNews.context.startActivity(
                    Intent(
                        holder.binding.cardNews.context,
                        ViewNoveltyActivity::class.java
                    ).apply {
                        putExtra(EXTRA_NOVELTY_ID, id)
                    }
                )
            }

            if (loggedIn) {
                holder.binding.icMenu.setOnClickListener {
                    val popupMenu = PopupMenu(it.context, holder.binding.icMenu)
                    popupMenu.inflate(R.menu.recyclerview_menu)

                    popupMenu.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.menu_delete -> {
                                AlertDialog.Builder(holder.itemView.context)
                                    .setTitle("Delete post")
                                    .setMessage("Do you really want to delete this post?")
                                    .setPositiveButton(
                                        "YES"
                                    ) { dialog, whichButton ->
                                        // delete event
                                        newsRepository.deleteNovelty(id)
                                    }.setNegativeButton(
                                        "NO"
                                    ) { dialog, whichButton ->
                                        // Do not delete
                                    }.show()
                            }
                            R.id.menu_edit -> {
                                newsRepository.updateNovelty()
                            }
                        }
                        true
                    }
                    popupMenu.show()
                }
            } else {
                holder.binding.icMenu.visibility = GONE
            }
        }

        override fun getItemCount() = news.size
    }
}
