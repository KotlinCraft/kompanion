package agent.domain

interface CodeApplier {

    fun apply(fileChange: FileChange) : Boolean

}