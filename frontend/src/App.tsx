import { useState } from 'react'
import './App.css'
import { UploadForm } from './components/UploadForm'
import { JobProgress } from './components/JobProgress'
import { ScoringConfigForm } from './components/ScoringConfigForm'
import { Dashboard } from './components/Dashboard'
import { MonitorPage } from './monitor/MonitorPage'
import type { JobResponse, ScoringSummaryResponse } from './api/types'

function App() {
  const [job, setJob] = useState<JobResponse | null>(null)
  const [scoringSummary, setScoringSummary] = useState<ScoringSummaryResponse | null>(null)
  const [showMonitor, setShowMonitor] = useState(false)

  function handleUploaded(newJob: JobResponse) {
    setJob(newJob)
    setScoringSummary(null)
  }

  function handleJobUpdate(updated: JobResponse) {
    setJob(updated)
  }

  function handleReset() {
    setJob(null)
    setScoringSummary(null)
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>TierForge</h1>
        <p>Resilient bulk store scoring &amp; tiering</p>
      </header>

      {showMonitor && <MonitorPage onBack={() => setShowMonitor(false)} />}

      {!showMonitor && !job && <UploadForm onUploaded={handleUploaded} />}

      {!showMonitor && job && (
        <>
          <button type="button" className="reset-link" onClick={handleReset}>
            ← Start a new job
          </button>
          <JobProgress job={job} onJobUpdate={handleJobUpdate} />
        </>
      )}

      {!showMonitor && (
        <button type="button" className="monitor-open-btn" onClick={() => setShowMonitor(true)}>
          Live monitor
        </button>
      )}

      {!showMonitor && job && job.status === 'COMPLETED' && (
        <ScoringConfigForm jobId={job.id} onScored={setScoringSummary} />
      )}

      {!showMonitor && job && scoringSummary && <Dashboard jobId={job.id} summary={scoringSummary} />}
    </div>
  )
}

export default App
