package com.routina.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.routina.app.databinding.ItemHabitBinding
import com.routina.app.model.Habit

class HabitAdapter(
    private val onToggleComplete: (Habit) -> Unit,
    private val onEditHabit: (Habit) -> Unit,
    private val onDeleteHabit: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HabitViewHolder {

        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HabitViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit) {

            binding.habitTitle.text = habit.title

            binding.streakText.text = habit.streak.toString()

            val goal = if (habit.goal <= 0) {
                1
            } else {
                habit.goal
            }

            binding.habitProgressBar.max = goal

            binding.habitProgressBar.progress =
                habit.progress.coerceAtMost(goal)

            binding.completeCheckbox.setOnCheckedChangeListener(null)

            binding.completeCheckbox.isChecked =
                habit.progress >= goal

            binding.completeCheckbox.setOnCheckedChangeListener { _, _ ->
                onToggleComplete(habit)
            }

            // Edit button
            binding.editHabitButton.setOnClickListener {
                onEditHabit(habit)
            }

            // Delete button
            binding.deleteHabitButton.setOnClickListener {
                onDeleteHabit(habit)
            }
        }
    }

    companion object {

        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Habit>() {

                override fun areItemsTheSame(
                    oldItem: Habit,
                    newItem: Habit
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Habit,
                    newItem: Habit
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
